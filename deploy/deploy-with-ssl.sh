#!/bin/bash

# 전체 배포 + SSL 자동 설정 스크립트

DOMAIN=${1:-byeolnight.com}
EMAIL=${2:-iusto@naver.com}

echo "🚀 별 헤는 밤 전체 배포 + SSL 설정 시작..."

# 1. 환경변수 파일 확인
if [ ! -f ".env" ]; then
    echo "❌ .env 파일이 없습니다. .env.example을 복사하여 설정하세요."
    exit 1
fi

# 2. Java 빌드
echo "☕ Java 애플리케이션 빌드 중..."
if [ -f "./gradlew" ]; then
    ./gradlew build -x test
else
    echo "⚠️ gradlew 없음. Docker 빌드로 진행합니다."
fi

# 3. HTTP로 먼저 배포
echo "🌐 HTTP 서비스 배포 중..."
docker-compose down --remove-orphans
docker-compose up --build -d

# 4. 서비스 시작 대기
echo "⏳ 서비스 시작 대기 중..."
sleep 30

# 5. HTTP 서비스 확인
echo "🏥 HTTP 서비스 헬스체크..."
for i in {1..5}; do
    if curl -f http://localhost > /dev/null 2>&1; then
        echo "✅ HTTP 서비스 정상"
        break
    fi
    echo "⏳ 대기 중... ($i/5)"
    sleep 10
done

# 6. 도메인이 설정된 경우에만 SSL 설정
if [ "$DOMAIN" != "localhost" ] && [ "$DOMAIN" != "127.0.0.1" ]; then
    echo "🔒 SSL 인증서 설정 시작..."
    
    # 도메인 연결 확인
    if ping -c 1 $DOMAIN > /dev/null 2>&1; then
        echo "✅ 도메인 연결 확인됨: $DOMAIN"
        
        # SSL 설정 실행
        chmod +x setup-ssl.sh
        ./setup-ssl.sh $DOMAIN $EMAIL
        
        if [ $? -eq 0 ]; then
            echo "🎉 HTTPS 배포 완료!"
            echo "🌐 서비스: https://$DOMAIN"
        else
            echo "⚠️ SSL 설정 실패. HTTP로 계속 서비스됩니다."
            echo "🌐 서비스: http://$DOMAIN"
        fi
    else
        echo "⚠️ 도메인 연결 실패: $DOMAIN"
        echo "🌐 HTTP 서비스: http://$DOMAIN"
    fi
else
    echo "🌐 로컬 HTTP 서비스: http://localhost"
fi

echo "📊 컨테이너 상태:"
docker-compose ps

echo "📚 API 문서: http://localhost:8080/swagger-ui.html"
echo "📊 모니터링: docker-compose logs -f"