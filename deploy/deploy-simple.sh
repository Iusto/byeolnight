#!/bin/bash

# byeolnight.com 간단 배포 스크립트 (Docker 빌드만 사용)

set -e

echo "🚀 byeolnight.com 배포를 시작합니다..."

# 1. 기존 서비스 중지
echo "🛑 기존 서비스를 중지합니다..."
docker-compose down 2>/dev/null || true

# 2. SSL 인증서 확인 및 발급
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

# 3. Docker로 빌드 및 시작 (Gradle 빌드는 Dockerfile에서 처리)
echo "🚀 서비스를 시작합니다..."
docker-compose up --build -d

# 4. 서비스 상태 확인
echo "⏳ 서비스 시작을 기다립니다..."
sleep 30

echo "📊 서비스 상태를 확인합니다..."
docker-compose ps

# 5. 헬스체크
echo "🏥 헬스체크를 수행합니다..."
if curl -f -s https://byeolnight.com/api/public/posts > /dev/null 2>&1; then
    echo "✅ 서비스가 정상적으로 실행 중입니다!"
    echo "🌐 https://byeolnight.com 으로 접속하세요."
else
    echo "⚠️ 헬스체크 실패. 서비스가 아직 시작 중일 수 있습니다."
    echo "📋 로그를 확인하세요: docker-compose logs app"
fi

# 6. 인증서 자동 갱신 설정
echo "🔄 SSL 인증서 자동 갱신을 설정합니다..."
(crontab -l 2>/dev/null | grep -v "certbot renew"; echo "0 12 * * * cd $(pwd) && docker run --rm -v /etc/letsencrypt:/etc/letsencrypt -v /var/www/certbot:/var/www/certbot certbot/certbot renew --quiet && docker-compose restart nginx") | crontab -

echo "🎉 배포가 완료되었습니다!"
echo "📅 SSL 인증서는 매일 12시에 자동으로 갱신됩니다."