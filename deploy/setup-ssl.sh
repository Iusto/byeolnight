#!/bin/bash

# SSL 인증서 자동 설정 스크립트

DOMAIN=${1:-byeolnight.com}
EMAIL=${2:-iusto@naver.com}

echo "🔒 SSL 인증서 자동 설정 시작..."
echo "도메인: $DOMAIN"
echo "이메일: $EMAIL"

# 1. HTTP로 서비스가 실행 중인지 확인
if ! curl -f http://localhost > /dev/null 2>&1; then
    echo "❌ HTTP 서비스가 실행되지 않았습니다. 먼저 docker-compose up -d를 실행하세요."
    exit 1
fi

echo "✅ HTTP 서비스 확인됨"

# 2. certbot으로 SSL 인증서 발급
echo "📜 SSL 인증서 발급 중..."
docker-compose run --rm certbot certonly \
    --webroot \
    --webroot-path=/var/www/certbot \
    --email $EMAIL \
    --agree-tos \
    --no-eff-email \
    -d $DOMAIN \
    -d www.$DOMAIN

if [ $? -ne 0 ]; then
    echo "❌ SSL 인증서 발급 실패"
    exit 1
fi

echo "✅ SSL 인증서 발급 완료"

# 3. nginx 설정을 HTTPS로 변경
echo "🔧 nginx 설정을 HTTPS로 변경 중..."

cat > nginx/nginx.conf << 'EOF'
events {
    worker_connections 1024;
}

http {
    upstream backend {
        server app:8080;
    }

    upstream frontend {
        server frontend:80;
    }

    # HTTP to HTTPS 리다이렉트
    server {
        listen 80;
        server_name byeolnight.com www.byeolnight.com;
        
        location /.well-known/acme-challenge/ {
            root /var/www/certbot;
        }
        
        location / {
            return 301 https://byeolnight.com$request_uri;
        }
    }

    # HTTPS 서버
    server {
        listen 443 ssl http2;
        server_name byeolnight.com www.byeolnight.com;

        ssl_certificate /etc/letsencrypt/live/byeolnight.com/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/byeolnight.com/privkey.pem;
        
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384;
        ssl_prefer_server_ciphers off;

        # API 요청
        location /api/ {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # WebSocket
        location /ws/ {
            proxy_pass http://backend;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # 프론트엔드
        location / {
            proxy_pass http://frontend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
}
EOF

# 4. docker-compose.yml을 HTTPS 지원으로 변경
echo "🔧 docker-compose.yml을 HTTPS 지원으로 변경 중..."

cat > docker-compose.yml << 'EOF'
# Docker Compose 설정 파일 (HTTPS 지원)

services:
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - /etc/letsencrypt:/etc/letsencrypt:ro
      - /var/www/certbot:/var/www/certbot
    depends_on:
      - app
      - frontend
    restart: unless-stopped

  certbot:
    image: certbot/certbot
    volumes:
      - /etc/letsencrypt:/etc/letsencrypt
      - /var/www/certbot:/var/www/certbot
    profiles: ["cert"]

  app:
    build: .
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
    env_file:
      - .env
    environment:
      - JAVA_OPTS=-Xmx2g -Xms1g -XX:+UseG1GC
    restart: unless-stopped
    volumes:
      - ./logs:/app/logs

  mysql:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: byeolnight
    ports:
      - "3306"
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    ports:
      - "6379"
    volumes:
      - redis_data:/data
    restart: unless-stopped

  frontend:
    build: ./byeolnight-frontend
    environment:
      - VITE_API_BASE_URL=${VITE_API_BASE_URL}
      - VITE_WS_URL=${VITE_WS_URL}
    depends_on:
      - app
    restart: unless-stopped

volumes:
  mysql_data:
  redis_data:
EOF

# 5. nginx 재시작
echo "🔄 nginx 재시작 중..."
docker-compose up -d nginx

# 6. SSL 인증서 자동 갱신 설정
echo "🔄 SSL 인증서 자동 갱신 설정 중..."
(crontab -l 2>/dev/null; echo "0 12 * * * cd $(pwd) && docker-compose run --rm certbot renew && docker-compose restart nginx") | crontab -

echo "🎉 SSL 설정 완료!"
echo "🌐 HTTPS 서비스: https://$DOMAIN"
echo "🔒 SSL 인증서는 매일 12시에 자동 갱신됩니다."