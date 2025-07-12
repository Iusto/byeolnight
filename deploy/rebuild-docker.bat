@echo off
echo 도커 컨테이너 중지 및 삭제...
docker-compose down

echo 도커 이미지 삭제...
docker rmi byeolnight-backend byeolnight-frontend 2>nul

echo 도커 빌드 캐시 정리...
docker builder prune -f

echo 도커 컨테이너 재빌드 및 실행...
docker-compose up --build -d

echo 완료! 서비스가 시작되었습니다.
echo 프론트엔드: http://localhost
echo 백엔드: http://localhost:8080
pause