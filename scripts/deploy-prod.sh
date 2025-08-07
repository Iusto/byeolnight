#!/bin/bash

# 배포 스크립트
set -e

echo "🚀 배포 시작..."

# 1. 최신 코드 가져오기
git pull origin master

# 2. 서비스 재시작
docker-compose down
docker-compose up -d --build

# 3. 상태 확인
sleep 20
if curl -f -s http://localhost:8080/actuator/health > /dev/null; then
    echo "✅ 배포 성공!"
else
    echo "❌ 배포 실패"
    exit 1
fi