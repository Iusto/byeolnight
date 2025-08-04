#!/bin/bash

echo "Config Server 재빌드 및 배포 스크립트"

echo "1. Config Server 빌드 중..."
cd config-server
./gradlew clean build -x test
if [ $? -ne 0 ]; then
    echo "Config Server 빌드 실패!"
    exit 1
fi

echo "2. Docker 이미지 빌드 중..."
docker build -t ghcr.io/iusto/byeolnight-config-server:latest .
if [ $? -ne 0 ]; then
    echo "Docker 이미지 빌드 실패!"
    exit 1
fi

echo "3. 기존 컨테이너 중지 및 제거..."
cd ..
docker-compose stop config-server
docker-compose rm -f config-server

echo "4. Config Server 재시작..."
docker-compose up -d config-server

echo "5. Config Server 상태 확인..."
sleep 10
docker-compose ps config-server
docker-compose logs --tail=20 config-server

echo "Config Server 재빌드 완료!"