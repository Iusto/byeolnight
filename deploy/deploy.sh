#!/bin/bash

# 별 헤는 밤 배포 스크립트
echo "🌌 별 헤는 밤 배포 시작..."

# 환경 변수 설정
export COMPOSE_PROJECT_NAME=byeolnight
export DOCKER_BUILDKIT=1

# 기존 컨테이너 정리
echo "📦 기존 컨테이너 정리 중..."
docker-compose down --remove-orphans

# 이미지 정리 (선택사항)
echo "🧹 사용하지 않는 이미지 정리..."
docker image prune -f

# 새 이미지 빌드 및 실행
echo "🔨 새 이미지 빌드 및 실행..."
docker-compose up --build -d

# 헬스체크
echo "🏥 서비스 상태 확인 중..."
sleep 30

# 서비스 상태 확인
if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ 배포 성공! 서비스가 정상 실행 중입니다."
    docker-compose ps
else
    echo "❌ 배포 실패! 로그를 확인하세요."
    docker-compose logs app
    exit 1
fi

echo "🎉 배포 완료!"