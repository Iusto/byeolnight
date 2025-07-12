#!/bin/bash

set -e  # 오류 발생 시 스크립트 중단

echo "🚀 별 헤는 밤 배포 시작..."
echo "📅 배포 시간: $(date)"

# 환경변수 파일 확인
if [ ! -f ".env" ]; then
    echo "❌ .env 파일이 없습니다. .env.example을 복사하여 설정하세요."
    exit 1
fi

# Git 최신 코드 가져오기 (선택적)
if [ "$1" = "--pull" ]; then
    echo "📚 최신 코드 업데이트 중..."
    git pull origin main
fi

# Java 빌드 (JAR 파일 생성)
echo "☕ Java 애플리케이션 빌드 중..."
if command -v ./gradlew &> /dev/null; then
    ./gradlew build -x test
elif command -v gradle &> /dev/null; then
    gradle build -x test
else
    echo "❌ Gradle을 찾을 수 없습니다."
    exit 1
fi

# 빌드 결과 확인
if [ ! -f build/libs/*.jar ]; then
    echo "❌ JAR 파일 빌드 실패"
    exit 1
fi

# 기존 컨테이너 중지 및 제거
echo "📦 기존 컨테이너 정리 중..."
docker-compose down --remove-orphans -v

# 사용하지 않는 이미지 정리
echo "🧹 사용하지 않는 Docker 이미지 정리 중..."
docker image prune -f

# 이미지 빌드 및 실행
echo "🔨 Docker 이미지 빌드 중..."
docker-compose up --build -d

# 컨테이너 시작 대기
echo "⏳ 컨테이너 시작 대기 중..."
sleep 45

# 컨테이너 상태 확인
echo "✅ 컨테이너 상태 확인..."
docker-compose ps

# 로그 확인
echo "📋 최근 로그 확인..."
docker-compose logs --tail=30 app

# 헬스체크
echo "🏥 서비스 헬스체크..."
max_attempts=10
attempt=1

while [ $attempt -le $max_attempts ]; do
    echo "🔍 헬스체크 시도 $attempt/$max_attempts..."
    
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ 백엔드 서비스 정상"
        backend_ok=true
        break
    else
        echo "⏳ 백엔드 서비스 대기 중... ($attempt/$max_attempts)"
        sleep 10
        attempt=$((attempt + 1))
    fi
done

if [ "$backend_ok" != "true" ]; then
    echo "❌ 백엔드 서비스 응답 없음"
    echo "📋 백엔드 로그:"
    docker-compose logs --tail=50 app
    exit 1
fi

if curl -f http://localhost > /dev/null 2>&1; then
    echo "✅ 프론트엔드 서비스 정상"
else
    echo "❌ 프론트엔드 서비스 응답 없음"
    echo "📋 프론트엔드 로그:"
    docker-compose logs --tail=20 frontend
fi

echo "🎉 배포 완료!"
echo "🌐 서비스 접속: http://localhost"
echo "📚 API 문서: http://localhost:8080/swagger-ui.html"
echo "📊 모니터링: docker-compose logs -f"
echo "🔧 사용법: $0 [--pull] (--pull: Git 최신 코드 가져오기)"