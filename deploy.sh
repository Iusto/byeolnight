#!/bin/bash

set -e

echo "🚀 별 헤는 밤 배포 시작..."

# 환경변수 파일 확인
if [ ! -f ".env" ]; then
    echo "❌ .env 파일이 없습니다. .env.example을 복사하여 설정하세요."
    exit 1
fi

# Java 빌드
echo "☕ Java 애플리케이션 빌드 중..."
if [ -f "./gradlew" ]; then
    ./gradlew build -x test
else
    echo "⚠️ gradlew 없음. Docker 빌드로 진행합니다."
fi

# 기존 컨테이너 정리
echo "📦 기존 컨테이너 정리 중..."
docker-compose down --remove-orphans
docker image prune -f

# 서비스 시작
echo "🔨 Docker 이미지 빌드 및 실행..."
docker-compose up --build -d

# 헬스체크
echo "⏳ 서비스 시작 대기 중..."
sleep 30

echo "🏥 헬스체크 수행 중..."
for i in {1..5}; do
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ 백엔드 서비스 정상"
        break
    fi
    echo "⏳ 대기 중... ($i/5)"
    sleep 10
done

if curl -f http://localhost > /dev/null 2>&1; then
    echo "✅ 프론트엔드 서비스 정상"
fi

echo "📊 컨테이너 상태:"
docker-compose ps

echo ""
echo "🎉 배포 완료!"
echo "🌐 서비스: http://localhost"
echo "📚 API 문서: http://localhost:8080/swagger-ui.html"
echo "📊 모니터링: docker-compose logs -f"
echo ""
echo "💡 SSL 설정을 원하면: cd deploy && ./deploy-with-ssl.sh your-domain.com your-email@example.com"