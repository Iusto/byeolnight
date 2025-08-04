@echo off
echo Config Server 재빌드 및 배포 스크립트

echo 1. Config Server 빌드 중...
cd config-server
call gradlew clean build -x test
if %ERRORLEVEL% neq 0 (
    echo Config Server 빌드 실패!
    pause
    exit /b 1
)

echo 2. Docker 이미지 빌드 중...
docker build -t ghcr.io/iusto/byeolnight-config-server:latest .
if %ERRORLEVEL% neq 0 (
    echo Docker 이미지 빌드 실패!
    pause
    exit /b 1
)

echo 3. 기존 컨테이너 중지 및 제거...
cd ..
docker-compose stop config-server
docker-compose rm -f config-server

echo 4. Config Server 재시작...
docker-compose up -d config-server

echo 5. Config Server 상태 확인...
timeout /t 10
docker-compose ps config-server
docker-compose logs --tail=20 config-server

echo Config Server 재빌드 완료!
pause