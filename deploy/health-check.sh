#!/bin/bash

echo "🏥 서비스 헬스체크 시작..."

# 백엔드 헬스체크
if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ 백엔드: 정상"
else
    echo "❌ 백엔드: 응답 없음"
    docker-compose logs --tail=5 app
fi

# 프론트엔드 헬스체크
if curl -f http://localhost > /dev/null 2>&1; then
    echo "✅ 프론트엔드: 정상"
else
    echo "❌ 프론트엔드: 응답 없음"
fi

# 데이터베이스 헬스체크
if docker-compose exec -T mysql mysqladmin ping -h localhost > /dev/null 2>&1; then
    echo "✅ MySQL: 정상"
else
    echo "❌ MySQL: 응답 없음"
fi

# Redis 헬스체크
if docker-compose exec -T redis redis-cli ping > /dev/null 2>&1; then
    echo "✅ Redis: 정상"
else
    echo "❌ Redis: 응답 없음"
fi

echo "🏥 헬스체크 완료!"