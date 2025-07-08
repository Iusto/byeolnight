#!/bin/bash

echo "🚀 별 헤는 밤 배포 시작..."

# 기존 컨테이너 중지 및 제거
echo "📦 기존 컨테이너 정리 중..."
docker-compose down --remove-orphans

# 이미지 빌드 및 실행
echo "🔨 Docker 이미지 빌드 중..."
docker-compose up --build -d

# 컨테이너 상태 확인
echo "✅ 컨테이너 상태 확인..."
docker-compose ps

echo "🎉 배포 완료!"
echo "🌐 서비스 접속: http://your-ec2-public-ip"
echo "📚 API 문서: http://your-ec2-public-ip:8080/swagger-ui.html"