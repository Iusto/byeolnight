#!/bin/bash

# byeolnight.com 프로덕션 배포 스크립트 (HTTPS 포함)

set -e

echo "🚀 byeolnight.com 프로덕션 배포를 시작합니다..."

# 1. 기존 서비스 완전 정리
echo "🛑 기존 서비스를 완전히 정리합니다..."
# deploy 디렉토리에서 실행되므로 상위 디렉토리로 이동
if [[ $(basename $(pwd)) == "deploy" ]]; then
    cd ..
fi

# 모든 컴테이너 중지 및 제거
docker compose down 2>/dev/null || true
docker stop $(docker ps -aq) 2>/dev/null || true
docker rm $(docker ps -aq) 2>/dev/null || true

echo "✅ 기존 컴테이너 정리 완료"

# 2. 코드 빌드
echo "🔨 코드를 빌드합니다..."

if [ -f "gradlew" ]; then
    chmod +x gradlew
    ./gradlew build -x test
    echo "✅ Gradle 빌드 완료"
else
    echo "⚠️ gradlew 파일이 없습니다. Docker 빌드로 진행합니다."
fi

# 3. DNS 설정 확인
echo "🌐 DNS 설정을 확인합니다..."
echo "⚠️ byeolnight.com 도메인이 이 서버 IP로 연결되어 있는지 확인하세요."
echo "📍 현재 서버 IP: $(curl -s ifconfig.me)"
echo "🔍 DNS 확인: nslookup byeolnight.com"
echo "🚀 우선 HTTP로 배포합니다. DNS 설정 후 SSL을 추가하세요."

# 4. 프로덕션 서비스 시작
echo "🚀 프로덕션 서비스를 시작합니다..."
echo "📍 현재 디렉토리: $(pwd)"
echo "📁 파일 목록:"
ls -la | head -10

if [ -f "docker-compose.yml" ]; then
    echo "✅ docker-compose.yml 파일을 찾았습니다."
    docker compose up --build -d
else
    echo "❌ docker-compose.yml 파일을 찾을 수 없습니다."
    echo "🔍 파일 검색:"
    find . -name "docker-compose*.yml" -type f 2>/dev/null || echo "파일을 찾을 수 없습니다."
    exit 1
fi

# 5. 서비스 상태 확인
echo "⏳ 서비스 시작을 기다립니다..."
sleep 30

echo "📊 서비스 상태를 확인합니다..."
docker compose ps

# 6. 헬스체크
echo "🏥 헬스체크를 수행합니다..."
SERVER_IP=$(curl -s ifconfig.me)
if curl -f -s http://$SERVER_IP/api/public/posts > /dev/null 2>&1; then
    echo "✅ 서비스가 정상적으로 실행 중입니다!"
    echo "🌐 http://$SERVER_IP 또는 http://byeolnight.com 으로 접속하세요."
else
    echo "⚠️ 헬스체크 실패. 서비스가 아직 시작 중일 수 있습니다."
    echo "📋 로그를 확인하세요: docker compose logs app"
fi

echo "🎉 HTTP 배포가 완료되었습니다!"
echo ""
echo "📝 다음 단계:"
echo "1. byeolnight.com DNS A 레코드를 $(curl -s ifconfig.me)로 설정"
echo "2. DNS 전파 후 SSL 인증서 발급: ./deploy/add-ssl.sh"
echo "3. 접속 테스트: http://byeolnight.com"