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

# Config Server 연결 테스트
if ! curl -s -u config-admin:config-secret-2024 http://localhost:8888/actuator/health > /dev/null; then
    echo "❌ Config Server 연결 실패"
    exit 1
fi

# 암호화된 값 가져오기
CONFIG_RESPONSE=$(curl -s -u config-admin:config-secret-2024 http://localhost:8888/byeolnight/prod)
echo "Config Response: $CONFIG_RESPONSE" | head -c 200

MYSQL_ENCRYPTED=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source."spring.datasource.password"')
REDIS_ENCRYPTED=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source."spring.data.redis.password"')

echo "MySQL 암호화된 값: $MYSQL_ENCRYPTED"
echo "Redis 암호화된 값: $REDIS_ENCRYPTED"

# 암호화된 값 복호화
echo "🔓 비밀번호 복호화 중..."
export MYSQL_ROOT_PASSWORD=$(curl -s -u config-admin:config-secret-2024 -X POST \
  -H "Content-Type: text/plain" \
  -d "$MYSQL_ENCRYPTED" \
  http://localhost:8888/decrypt)
  
export REDIS_PASSWORD=$(curl -s -u config-admin:config-secret-2024 -X POST \
  -H "Content-Type: text/plain" \
  -d "$REDIS_ENCRYPTED" \
  http://localhost:8888/decrypt)

echo "MySQL 복호화된 값: [${#MYSQL_ROOT_PASSWORD}자]"
echo "Redis 복호화된 값: [${#REDIS_PASSWORD}자]"

if [ -z "$MYSQL_ROOT_PASSWORD" ] || [ -z "$REDIS_PASSWORD" ]; then
    echo "❌ 비밀번호 복호화 실패"
    exit 1
fi

echo "✅ 비밀번호 복호화 완료"

# 4. 전체 서비스 빌드 및 배포
echo "🏗️ 서비스 빌드 및 배포..."
docker compose build --no-cache && docker compose up -d

echo "✅ 배포 완료! 로그 확인 중..."
docker logs -f byeolnight-app-1