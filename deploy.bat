@echo off
echo 🚀 별 헤는 밤 배포 시작...

REM 환경변수 파일 확인
if not exist ".env" (
    echo ❌ .env 파일이 없습니다. .env.example을 복사하여 설정하세요.
    pause
    exit /b 1
)

REM Java 빌드
echo ☕ Java 애플리케이션 빌드 중...
call gradlew build -x test
if errorlevel 1 (
    echo ❌ Java 빌드 실패
    pause
    exit /b 1
)

REM 기존 컨테이너 정리
echo 📦 기존 컨테이너 정리 중...
docker-compose down --remove-orphans
docker image prune -f

REM 서비스 시작
echo 🔨 Docker 이미지 빌드 및 실행...
docker-compose up --build -d

REM 헬스체크
echo ⏳ 서비스 시작 대기 중...
timeout /t 30 /nobreak > nul

echo 🏥 헬스체크 수행 중...
for /l %%i in (1,1,5) do (
    curl -f http://localhost:8080/actuator/health > nul 2>&1
    if not errorlevel 1 (
        echo ✅ 백엔드 서비스 정상
        goto :frontend_check
    )
    echo ⏳ 대기 중... (%%i/5)
    timeout /t 10 /nobreak > nul
)

:frontend_check
curl -f http://localhost > nul 2>&1
if not errorlevel 1 (
    echo ✅ 프론트엔드 서비스 정상
)

echo 📊 컨테이너 상태:
docker-compose ps

echo.
echo 🎉 배포 완료!
echo 🌐 서비스: http://localhost
echo 📚 API 문서: http://localhost:8080/swagger-ui.html
echo 📊 모니터링: docker-compose logs -f
echo.
echo 💡 SSL 설정을 원하면: cd deploy && ./deploy-with-ssl.sh your-domain.com your-email@example.com
echo.
pause