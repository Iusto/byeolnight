#!/bin/bash
# Config Server 설정 상태 확인 스크립트

echo "=== Config Server 설정 확인 ==="

echo "1. Config Server 컨테이너 상태:"
docker ps | grep config-server

echo -e "\n2. Config-repo 디렉토리 존재 여부:"
ls -la config-repo/ 2>/dev/null || echo "config-repo 디렉토리 없음"

echo -e "\n3. Config 파일 존재 여부:"
ls -la config-repo/configs/ 2>/dev/null || echo "config-repo/configs 디렉토리 없음"

echo -e "\n4. Config Server 응답 테스트:"
curl -s -u config-admin:config-secret-2024 http://localhost:8888/byeolnight/prod | head -c 500

echo -e "\n\n5. 환경변수 상태:"
echo "MYSQL_ROOT_PASSWORD 길이: ${#MYSQL_ROOT_PASSWORD}"
echo "REDIS_PASSWORD 길이: ${#REDIS_PASSWORD}"