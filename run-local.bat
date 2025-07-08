@echo off
echo 로컬 개발 환경 시작...

echo 1. Docker 컨테이너 시작 (MySQL, Redis)
docker-compose -f docker-compose.local.yml up -d

echo 2. 컨테이너 상태 확인
docker-compose -f docker-compose.local.yml ps

echo 3. Spring Boot 애플리케이션 시작 (local 프로필)
echo "애플리케이션을 시작하려면 다음 명령어를 실행하세요:"
echo "gradlew bootRun --args='--spring.profiles.active=local'"
echo "또는 IDE에서 VM options에 -Dspring.profiles.active=local 추가"

pause