#!/bin/bash

set -e  # 오류 발생 시 스크립트 중단

echo "🚀 별 헤는 밤 배포 시작..."
echo "📅 배포 시간: $(date)"

# 환경변수 파일 확인
if [ ! -f ".env" ]; then
    echo "❌ .env 파일이 없습니다. .env.example을 복사하여 설정하세요."
    exit 1
fi

# Git 최신 코드 가져오기
echo "📚 최신 코드 업데이트 중..."
git pull origin main

# 기존 컨테이너 중지 및 제거
echo "📦 기존 컨테이너 정리 중..."
docker-compose down --remove-orphans

# 사용하지 않는 이미지 정리
echo "🧹 사용하지 않는 Docker 이미지 정리 중..."
docker image prune -f

# 이미지 빌드 및 실행
echo "🔨 Docker 이미지 빌드 중..."
docker-compose up --build -d

# 컨테이너 시작 대기
echo "⏳ 컨테이너 시작 대기 중..."
sleep 30

# 컨테이너 상태 확인
echo "✅ 컨테이너 상태 확인..."
docker-compose ps

# 로그 확인
echo "📋 최근 로그 확인..."
docker-compose logs --tail=20

# 헬스체크
echo "🏥 서비스 헬스체크..."
if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ 백엔드 서비스 정상"
else
    echo "❌ 백엔드 서비스 응답 없음"
fi

if curl -f http://localhost > /dev/null 2>&1; then
    echo "✅ 프론트엔드 서비스 정상"
else
    echo "❌ 프론트엔드 서비스 응답 없음"
fi

echo "🎉 배포 완료!"
echo "🌐 서비스 접속: http://$(curl -s http://checkip.amazonaws.com)"
echo "📚 API 문서: http://$(curl -s http://checkip.amazonaws.com):8080/swagger-ui.html"
echo "📊 모니터링: docker-compose logs -f"