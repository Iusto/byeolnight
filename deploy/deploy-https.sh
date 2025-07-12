#!/bin/bash

# HTTPS 배포 스크립트

echo "🚀 byeolnight.com HTTPS 배포를 시작합니다..."

# 1. 기존 컨테이너 정리
echo "🧹 기존 컨테이너를 정리합니다..."
docker-compose down

# 2. 최신 코드 빌드
echo "🔨 최신 코드를 빌드합니다..."
./gradlew build -x test

# 3. 임시 HTTP 서버로 인증서 발급
echo "📋 임시 HTTP 서버를 시작합니다..."
docker run -d --name temp-nginx \
    -p 80:80 \
    -v /var/www/certbot:/var/www/certbot \
    -v $(pwd)/nginx/nginx-temp.conf:/etc/nginx/nginx.conf \
    nginx:alpine

# 임시 Nginx 설정 생성
cat > nginx/nginx-temp.conf << 'EOF'
events {
    worker_connections 1024;
}

http {
    server {
        listen 80;
        server_name byeolnight.com www.byeolnight.com;
        
        location /.well-known/acme-challenge/ {
            root /var/www/certbot;
        }
        
        location / {
            return 200 'OK';
            add_header Content-Type text/plain;
        }
    }
}
EOF

# 4. SSL 인증서 발급
echo "🔐 SSL 인증서를 발급받습니다..."
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

# 5. 임시 서버 정리
echo "🧹 임시 서버를 정리합니다..."
docker stop temp-nginx
docker rm temp-nginx
rm nginx/nginx-temp.conf

# 6. HTTPS 서비스 시작
echo "🚀 HTTPS 서비스를 시작합니다..."
docker-compose up --build -d

echo "✅ HTTPS 배포가 완료되었습니다!"
echo "🌐 https://byeolnight.com 으로 접속하세요."

# 7. 상태 확인
sleep 10
echo "📊 서비스 상태를 확인합니다..."
docker-compose ps