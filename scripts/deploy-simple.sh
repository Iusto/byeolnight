#!/bin/bash

echo "🚀 배포 시작..."

git fetch origin master && git reset --hard origin/master && \
chmod +x ./gradlew && ./gradlew clean build -x test && \
docker-compose down && \
docker system prune -f && \
docker-compose build --no-cache && \
docker-compose up -d

echo "✅ 배포 완료!"