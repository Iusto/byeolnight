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
sleep 15

# Config Server 상태 확인
echo "🔍 Config Server 상태 확인..."
for i in {1..10}; do
    if curl -s -u config-admin:config-secret-2024 http://localhost:8888/actuator/health > /dev/null 2>&1; then
        echo "✅ Config Server 준비 완료"
        break
    fi
    echo "⏳ Config Server 대기 중... ($i/10)"
    sleep 3
done

echo "🔑 Config Server에서 비밀번호 가져오기..."
# 암호화된 값 가져오기
CONFIG_RESPONSE=$(curl -s -u config-admin:config-secret-2024 http://localhost:8888/byeolnight/prod)
MYSQL_ENCRYPTED=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source."spring.datasource.password"')
REDIS_ENCRYPTED=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source."spring.data.redis.password"')

# 암호화된 값 복호화 시도
echo "🔓 비밀번호 복호화 중..."
MYSQL_DECRYPTED=$(curl -s -u config-admin:config-secret-2024 -X POST \
  -H "Content-Type: text/plain" \
  -d "$MYSQL_ENCRYPTED" \
  http://localhost:8888/decrypt)
  
REDIS_DECRYPTED=$(curl -s -u config-admin:config-secret-2024 -X POST \
  -H "Content-Type: text/plain" \
  -d "$REDIS_ENCRYPTED" \
  http://localhost:8888/decrypt)

# 복호화 실패 시 하드코딩된 값 사용
if [[ "$MYSQL_DECRYPTED" == *"INVALID"* ]] || [[ "$REDIS_DECRYPTED" == *"INVALID"* ]]; then
    echo "⚠️ 복호화 실패, 기본값 사용"
    MYSQL_ROOT_PASSWORD="byeolnight2024!"
    REDIS_PASSWORD="byeolnight2024!"
else
    MYSQL_ROOT_PASSWORD="$MYSQL_DECRYPTED"
    REDIS_PASSWORD="$REDIS_DECRYPTED"
fi

echo "환경변수 확인:"
echo "MYSQL_ROOT_PASSWORD=[${#MYSQL_ROOT_PASSWORD}자] 설정됨"
echo "REDIS_PASSWORD=[${#REDIS_PASSWORD}자] 설정됨"
echo "✅ 비밀번호 설정 완료"

export MYSQL_ROOT_PASSWORD
export REDIS_PASSWORD

# 4. 전체 서비스 빌드 및 배포
echo "🏗️ 서비스 빌드 및 배포..."
echo "현재 환경변수 상태:"
echo "MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD:0:3}***"
echo "REDIS_PASSWORD=${REDIS_PASSWORD:0:3}***"

# .env 파일 생성 (Docker Compose가 자동으로 읽음)
cat > .env << EOF
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
REDIS_PASSWORD=${REDIS_PASSWORD}
EOF

docker compose build --no-cache && docker compose up -d

echo "✅ 배포 완료! 로그 확인 중..."
docker logs -f byeolnight-app-1