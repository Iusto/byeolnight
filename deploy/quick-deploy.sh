#!/bin/bash

# 빠른 배포 스크립트 (개발용)
set -e

echo "⚡ 빠른 배포 시작..."

# 기존 컨테이너만 재시작 (이미지 빌드 스킵)
echo "🔄 컨테이너 재시작 중..."
docker-compose restart

# 상태 확인
echo "✅ 컨테이너 상태:"
docker-compose ps

echo "⚡ 빠른 배포 완료!"