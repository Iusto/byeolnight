#!/bin/bash
# 데이터베이스 스키마 문제 해결

echo "🔧 데이터베이스 스키마 문제 해결 중..."

# 현재 설정 확인
echo "현재 DDL 설정 확인:"
curl -s -u config-admin:config-secret-2024 http://localhost:8888/byeolnight/prod | jq -r '.propertySources[0].source."spring.jpa.hibernate.ddl-auto"'

# 임시로 DDL을 update로 변경하여 테이블 생성
echo "🔄 컨테이너 재시작 (DDL=update)..."
docker compose down

# 환경변수 다시 설정
CONFIG_RESPONSE=$(curl -s -u config-admin:config-secret-2024 http://localhost:8888/byeolnight/prod)
MYSQL_ENCRYPTED=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source."spring.datasource.password"')
REDIS_ENCRYPTED=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source."spring.data.redis.password"')

export MYSQL_ROOT_PASSWORD="$MYSQL_ENCRYPTED"
export REDIS_PASSWORD="$REDIS_ENCRYPTED"

# DDL을 update로 설정하여 시작
export SPRING_JPA_HIBERNATE_DDL_AUTO=update

docker compose up -d

echo "✅ 스키마 업데이트 모드로 시작 완료!"
echo "애플리케이션이 시작되면 테이블이 자동 생성됩니다."