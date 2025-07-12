#!/bin/bash

# byeolnight.com 프로덕션 배포 스크립트 (HTTPS 포함)

set -e

echo "🚀 byeolnight.com 프로덕션 배포를 시작합니다..."

# 1. 기존 서비스 중지
echo "🛑 기존 서비스를 중지합니다..."
docker-compose down 2>/dev/null || true

# 2. 최신 코드 빌드
echo "🔨 최신 코드를 빌드합니다..."
cd ..
./gradlew clean build -x test

# 3. SSL 인증서 확인 및 발급
echo "🔐 SSL 인증서를 확인합니다..."
if [ ! -f "/etc/letsencrypt/live/byeolnight.com/fullchain.pem" ]; then
    echo "📋 SSL 인증서를 발급받습니다..."
    
    # 임시 HTTP 서버 시작
    docker run -d --name temp-nginx \
        -p 80:80 \
        -v /var/www/certbot:/var/www/certbot \
        nginx:alpine
    
    # 임시 Nginx 설정
    docker exec temp-nginx sh -c 'cat > /etc/nginx/conf.d/default.conf << EOF
server {
    listen 80;
    server_name byeolnight.com www.byeolnight.com;
    
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    
    location / {
        return 200 "OK";
        add_header Content-Type text/plain;
    }
}
EOF'
    
    docker exec temp-nginx nginx -s reload
    
    # SSL 인증서 발급
    docker run --rm \
        -v /etc/letsencrypt:/etc/letsencrypt \
        -v /var/www/certbot:/var/www/certbot \
        certbot/certbot certonly \
        --webroot \
        --webroot-path=/var/www/certbot \
        --email iusto@naver.com \
        --agree-tos \
        --no-eff-email \
        -d byeolnight.com \
        -d www.byeolnight.com
    
    # 임시 서버 정리
    docker stop temp-nginx
    docker rm temp-nginx
    
    echo "✅ SSL 인증서 발급 완료!"
else
    echo "✅ SSL 인증서가 이미 존재합니다."
fi

# 4. 프로덕션 서비스 시작
echo "🚀 프로덕션 서비스를 시작합니다..."
docker-compose up --build -d

# 5. 서비스 상태 확인
echo "⏳ 서비스 시작을 기다립니다..."
sleep 30

echo "📊 서비스 상태를 확인합니다..."
docker-compose ps

# 6. 헬스체크
echo "🏥 헬스체크를 수행합니다..."
if curl -f -s https://byeolnight.com/api/public/posts > /dev/null; then
    echo "✅ 서비스가 정상적으로 실행 중입니다!"
    echo "🌐 https://byeolnight.com 으로 접속하세요."
else
    echo "❌ 서비스 헬스체크 실패. 로그를 확인하세요."
    docker-compose logs app
    exit 1
fi

# 7. 인증서 자동 갱신 설정
echo "🔄 SSL 인증서 자동 갱신을 설정합니다..."
(crontab -l 2>/dev/null | grep -v "certbot renew"; echo "0 12 * * * cd $(pwd) && docker run --rm -v /etc/letsencrypt:/etc/letsencrypt -v /var/www/certbot:/var/www/certbot certbot/certbot renew --quiet && docker-compose restart nginx") | crontab -

echo "🎉 배포가 완료되었습니다!"
echo "📅 SSL 인증서는 매일 12시에 자동으로 갱신됩니다."