#!/bin/bash

set -e

# 환경 변수 설정
REGISTRY="ghcr.io"
REPO_NAME="iusto/byeolnight"
IMAGE_TAG=${IMAGE_TAG:-latest}
COMPOSE_PROJECT="byeolnight"

# 색상 출력
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}🚀 무중단 배포 시작 (이미지 태그: ${IMAGE_TAG})${NC}"

# GitHub Container Registry 로그인
echo "$GITHUB_TOKEN" | docker login ghcr.io -u "$GITHUB_USERNAME" --password-stdin

# 새 이미지 Pull
echo -e "${YELLOW}📥 새 이미지 다운로드 중...${NC}"
docker pull ${REGISTRY}/${REPO_NAME}:master-${IMAGE_TAG}

# 현재 실행 중인 컨테이너 확인
CURRENT_CONTAINER=$(docker ps --filter "name=${COMPOSE_PROJECT}-app" --format "{{.Names}}" | head -1)

if [ -z "$CURRENT_CONTAINER" ]; then
    echo -e "${YELLOW}⚠️  실행 중인 컨테이너가 없습니다. 일반 배포를 진행합니다.${NC}"
    
    # 환경 변수 설정하여 docker-compose 실행
    export APP_IMAGE=${REGISTRY}/${REPO_NAME}:master-${IMAGE_TAG}
    docker-compose -f docker-compose.prod.yml up -d
    
    echo -e "${GREEN}✅ 배포 완료${NC}"
    exit 0
fi

echo -e "${YELLOW}🔄 무중단 배포 진행 중...${NC}"

# 새 컨테이너를 다른 포트로 실행
export APP_IMAGE=${REGISTRY}/${REPO_NAME}:master-${IMAGE_TAG}
export APP_PORT=8081
docker-compose -f docker-compose.prod.yml -p ${COMPOSE_PROJECT}-new up -d app

# 헬스 체크
echo -e "${YELLOW}🏥 헬스 체크 중...${NC}"
for i in {1..30}; do
    if curl -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 새 컨테이너 정상 실행 확인${NC}"
        break
    fi
    
    if [ $i -eq 30 ]; then
        echo -e "${RED}❌ 헬스 체크 실패. 배포를 중단합니다.${NC}"
        docker-compose -p ${COMPOSE_PROJECT}-new down
        exit 1
    fi
    
    echo "헬스 체크 시도 $i/30..."
    sleep 10
done

# Nginx 설정 업데이트 (포트 8080 → 8081)
echo -e "${YELLOW}🔄 트래픽 전환 중...${NC}"
sudo sed -i 's/proxy_pass http:\/\/localhost:8080/proxy_pass http:\/\/localhost:8081/' /etc/nginx/sites-available/byeolnight
sudo nginx -t && sudo systemctl reload nginx

# 기존 컨테이너 종료
echo -e "${YELLOW}🛑 기존 컨테이너 종료 중...${NC}"
docker-compose -f docker-compose.prod.yml -p ${COMPOSE_PROJECT} down

# 새 컨테이너를 원래 포트로 재시작
echo -e "${YELLOW}🔄 새 컨테이너를 포트 8080으로 재시작 중...${NC}"
export APP_PORT=8080
docker-compose -f docker-compose.prod.yml -p ${COMPOSE_PROJECT} up -d app

# 헬스 체크
for i in {1..15}; do
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 포트 8080에서 정상 실행 확인${NC}"
        break
    fi
    sleep 5
done

# Nginx 설정 원복 (포트 8081 → 8080)
sudo sed -i 's/proxy_pass http:\/\/localhost:8081/proxy_pass http:\/\/localhost:8080/' /etc/nginx/sites-available/byeolnight
sudo nginx -t && sudo systemctl reload nginx

# 임시 컨테이너 정리
docker-compose -p ${COMPOSE_PROJECT}-new down

echo -e "${GREEN}🎉 무중단 배포 완료!${NC}"