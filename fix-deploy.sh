#!/bin/bash
# 환경변수 설정 및 배포 수정 스크립트

set -e

echo "🔧 환경변수 설정 및 배포 수정..."

# Config Server에서 암호화된 값 가져오기
echo "📡 Config Server에서 설정 가져오기..."
CONFIG_RESPONSE=$(curl -s -u config-admin:config-secret-2024 http://localhost:8888/byeolnight/prod)

# 암호화된 값 추출
MYSQL_ENCRYPTED=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source."spring.datasource.password"')
REDIS_ENCRYPTED=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source."spring.data.redis.password"')

echo "MySQL 암호화된 값: $MYSQL_ENCRYPTED"
echo "Redis 암호화된 값: $REDIS_ENCRYPTED"

# 복호화 (암호화된 값인 경우에만)
if [[ $MYSQL_ENCRYPTED == \{cipher\}* ]]; then
    echo "🔓 MySQL 비밀번호 복호화 중..."
    MYSQL_DECRYPTED=$(curl -s -u config-admin:config-secret-2024 -X POST \
      -H "Content-Type: text/plain" \
      -d "$MYSQL_ENCRYPTED" \
      http://localhost:8888/decrypt)
else
    echo "⚠️ MySQL 비밀번호가 평문입니다."
    MYSQL_DECRYPTED="$MYSQL_ENCRYPTED"
fi

if [[ $REDIS_ENCRYPTED == \{cipher\}* ]]; then
    echo "🔓 Redis 비밀번호 복호화 중..."
    REDIS_DECRYPTED=$(curl -s -u config-admin:config-secret-2024 -X POST \
      -H "Content-Type: text/plain" \
      -d "$REDIS_ENCRYPTED" \
      http://localhost:8888/decrypt)
else
    echo "⚠️ Redis 비밀번호가 평문입니다."
    REDIS_DECRYPTED="$REDIS_ENCRYPTED"
fi

# 환경변수 설정
export MYSQL_ROOT_PASSWORD="$MYSQL_DECRYPTED"
export REDIS_PASSWORD="$REDIS_DECRYPTED"

echo "✅ 환경변수 설정 완료:"
echo "MYSQL_ROOT_PASSWORD=[${#MYSQL_ROOT_PASSWORD}자]"
echo "REDIS_PASSWORD=[${#REDIS_PASSWORD}자]"

# Docker Compose 재시작
echo "🔄 Docker Compose 재시작..."
docker compose down
docker compose up -d

echo "✅ 배포 완료!"