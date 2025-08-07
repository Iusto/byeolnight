#!/bin/bash

# 배포 디버그 스크립트
echo "🔍 배포 상태 확인..."

# 1. 컨테이너 상태 확인
echo "📊 컨테이너 상태:"
docker-compose ps

# 2. Config Server 로그 확인
echo "📋 Config Server 로그:"
docker-compose logs config-server --tail=50

# 3. 네트워크 확인
echo "🌐 네트워크 상태:"
docker network ls | grep byeolnight

# 4. 포트 확인
echo "🔌 포트 상태:"
netstat -tlnp | grep :8888 || echo "포트 8888이 열려있지 않음"