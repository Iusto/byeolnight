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

# 3. Config Server 시작 및 환경변수 설정
echo "⚙️ Config Server 시작..."
docker compose up config-server -d
echo "⏳ Config Server 준비 대기..."
sleep 10

echo "🔑 Config Server에서 비밀번호 가져오기..."
# 암호화된 값 가져오기
CONFIG_RESPONSE=$(curl -s -u config-admin:config-secret-2024 http://localhost:8888/byeolnight/prod)
MYSQL_ENCRYPTED=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source."spring.datasource.password"')
REDIS_ENCRYPTED=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source."spring.data.redis.password"')

# 암호화된 값 복호화
echo "🔓 비밀번호 복호화 중..."
# MySQL 비밀번호 복호화
MYSQL_ROOT_PASSWORD=$(curl -s -u config-admin:config-secret-2024 -X POST \
  -H "Content-Type: text/plain" \
  -d "$MYSQL_ENCRYPTED" \
  http://localhost:8888/decrypt)
  
# Redis 비밀번호 복호화
REDIS_PASSWORD=$(curl -s -u config-admin:config-secret-2024 -X POST \
  -H "Content-Type: text/plain" \
  -d "$REDIS_ENCRYPTED" \
  http://localhost:8888/decrypt)

# 복호화 결과 검증
echo "환경변수 확인:"
echo "MYSQL_ROOT_PASSWORD=[${#MYSQL_ROOT_PASSWORD}자] 설정됨"
echo "REDIS_PASSWORD=[${#REDIS_PASSWORD}자] 설정됨"

# 빈 값이나 에러 메시지 체크
if [ -z "$MYSQL_ROOT_PASSWORD" ] || [ -z "$REDIS_PASSWORD" ] || \
   [[ "$MYSQL_ROOT_PASSWORD" == *"error"* ]] || [[ "$REDIS_PASSWORD" == *"error"* ]]; then
    echo "❌ 비밀번호 복호화 실패"
    echo "MYSQL: $MYSQL_ROOT_PASSWORD"
    echo "REDIS: $REDIS_PASSWORD"
    exit 1
fi

echo "✅ 비밀번호 복호화 완료"

# 환경변수 내보내기
export MYSQL_ROOT_PASSWORD
export REDIS_PASSWORD

# 4. 전체 서비스 빌드 및 배포
echo "🏗️ 서비스 빌드 및 배포..."
echo "현재 환경변수 상태:"
echo "MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD:0:3}***"
echo "REDIS_PASSWORD=${REDIS_PASSWORD:0:3}***"

# 환경변수와 함께 Docker Compose 실행
MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" REDIS_PASSWORD="$REDIS_PASSWORD" \
docker compose build --no-cache && \
MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" REDIS_PASSWORD="$REDIS_PASSWORD" \
docker compose up -d

echo "✅ 배포 완료! 로그 확인 중..."
docker logs -f byeolnight-app-1