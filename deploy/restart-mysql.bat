@echo off
echo MySQL 포트 노출을 위해 컨테이너 재시작...
docker-compose down mysql
docker-compose up -d mysql

echo MySQL 컨테이너 상태 확인...
docker-compose ps mysql

echo 완료! MySQL이 3306 포트로 외부 접근 가능합니다.
pause