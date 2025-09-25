#!/bin/bash
# EC2 서버 원클릭 배포 스크립트
# 사용법: chmod +x deploy.sh && ./deploy.sh

set -e

echo "🚀 별 헤는 밤 배포 시작..."
echo "📦 패키지 매니저: pnpm (프론트엔드)"

# 0. 포트 충돌 방지 (최우선)
echo "🔧 포트 충돌 방지 및 기존 프로세스 정리..."
sudo pkill -f nginx || true
sudo fuser -k 80/tcp 443/tcp || true
# 80포트 사용 프로세스가 있을 때만 kill
if sudo lsof -ti:80 2>/dev/null; then
    sudo lsof -ti:80 | xargs sudo kill -9 || true
fi
echo "✅ 포트 정리 완료"

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
sleep 20

# Config Server 상태 확인
echo "🔍 Config Server 상태 확인..."
for i in {1..15}; do
    if curl -s -u config-admin:config-secret-2024 http://localhost:8888/actuator/health > /dev/null 2>&1; then
        # 암호화 기능 검증
        if curl -s -X POST http://localhost:8888/encrypt -d "test" | grep -q "AQA"; then
            echo "✅ Config Server 준비 완료 (암호화 기능 확인)"
            break
        else
            echo "⚠️ Config Server 암호화 기능 대기 중... ($i/15)"
        fi
    else
        echo "⏳ Config Server 대기 중... ($i/15)"
    fi
    sleep 3
done



echo "🔑 Config Server에서 비밀번호 가져오기..."
# 평문 값 가져오기
CONFIG_RESPONSE=$(curl -s -u config-admin:config-secret-2024 http://localhost:8888/byeolnight/prod)
MYSQL_ROOT_PASSWORD=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source."docker.mysql.root-password"')
REDIS_PASSWORD=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source."docker.redis.password"')

if [[ -z "$MYSQL_ROOT_PASSWORD" ]] || [[ -z "$REDIS_PASSWORD" ]] || [[ "$MYSQL_ROOT_PASSWORD" == "null" ]] || [[ "$REDIS_PASSWORD" == "null" ]]; then
    echo "❌ Config Server에서 비밀번호를 가져오지 못했습니다"
    exit 1
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

# 5. SSL 인증서 갱신 체크 (재부팅 시)
echo "🔒 SSL 인증서 상태 확인..."
if sudo certbot certificates 2>/dev/null | grep -q "byeolnight.com"; then
    echo "📋 SSL 인증서 갱신 체크..."
    # nginx 중지 후 갱신 시도
    docker compose stop nginx
    sudo certbot renew --quiet || echo "⚠️ SSL 갱신 불필요 또는 실패"
    docker compose start nginx
    echo "✅ SSL 인증서 체크 완료"
else
    echo "⚠️ SSL 인증서가 설치되지 않음"
fi

echo "✅ 배포 완료! 로그 확인 중..."
docker logs -f byeolnight-app-1