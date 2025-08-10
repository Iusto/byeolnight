#!/bin/bash
# EC2 서버 원클릭 배포 스크립트
# 사용법: chmod +x deploy.sh && ./deploy.sh

set -e

echo "🚀 별 헤는 밤 배포 시작..."

# 1. 코드 업데이트 및 빌드
echo "📥 최신 코드 가져오기..."
git fetch origin master && git reset --hard origin/master
echo "🔨 애플리케이션 빌드..."
chmod +x ./gradlew && ./gradlew clean bootJar -x test

# 2. 기존 컨테이너 정리
echo "🧹 기존 컨테이너 정리..."
docker compose down
docker system prune -f

# 3. Config Server 시작 및 환경변수 설정
echo "⚙️ Config Server 시작..."
docker compose up config-server -d
echo "⏳ Config Server 준비 대기..."
sleep 10

echo "🔑 Config Server에서 비밀번호 가져오기..."
CONFIG_RESPONSE=$(curl -s -u config-admin:config-secret-2024 http://localhost:8888/byeolnight/prod)
export MYSQL_ROOT_PASSWORD=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source."spring.datasource.password"')
export REDIS_PASSWORD=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source."spring.data.redis.password"')

# 4. 전체 서비스 빌드 및 배포
echo "🏗️ 서비스 빌드 및 배포..."
docker compose build --no-cache && docker compose up -d

echo "✅ 배포 완료! 로그 확인 중..."
docker logs -f byeolnight-app-1