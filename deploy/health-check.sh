#!/bin/bash

echo "🏥 서비스 헬스체크 시작..."

# 백엔드 헬스체크
echo "🔍 백엔드 서비스 확인..."
if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ 백엔드: 정상"
    if command -v jq &> /dev/null; then
        curl -s http://localhost:8080/actuator/health | jq .
    else
        curl -s http://localhost:8080/actuator/health
    fi
else
    echo "❌ 백엔드: 응답 없음"
    echo "📋 백엔드 로그:"
    docker-compose logs --tail=10 app
fi

echo ""

# 프론트엔드 헬스체크
echo "🔍 프론트엔드 서비스 확인..."
if curl -f http://localhost > /dev/null 2>&1; then
    echo "✅ 프론트엔드: 정상"
else
    echo "❌ 프론트엔드: 응답 없음"
    echo "📋 프론트엔드 로그:"
    docker-compose logs --tail=10 frontend
fi

echo ""

# 데이터베이스 헬스체크
echo "🔍 데이터베이스 서비스 확인..."
if docker-compose exec -T mysql mysqladmin ping -h localhost > /dev/null 2>&1; then
    echo "✅ MySQL: 정상"
else
    echo "❌ MySQL: 응답 없음"
    echo "📋 MySQL 로그:"
    docker-compose logs --tail=10 mysql
fi

echo ""

# Redis 헬스체크
echo "🔍 Redis 서비스 확인..."
if docker-compose exec -T redis redis-cli ping > /dev/null 2>&1; then
    echo "✅ Redis: 정상"
else
    echo "❌ Redis: 응답 없음"
    echo "📋 Redis 로그:"
    docker-compose logs --tail=10 redis
fi

echo ""
echo "🏥 헬스체크 완료!"