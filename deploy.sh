#!/bin/bash
# EC2 백엔드 배포 스크립트 (프론트엔드는 S3+CloudFront)
# 사용법: chmod +x deploy.sh && ./deploy.sh
set -euo pipefail

ROOT_DIR="/home/ubuntu/byeolnight"
cd "$ROOT_DIR" || { echo "❌ 디렉터리 이동 실패"; exit 1; }

mkdir -p logs
LOG_FILE="$ROOT_DIR/logs/deploy-$(date +%Y%m%d-%H%M%S).log"
exec 1> >(tee -a "$LOG_FILE")
exec 2>&1

echo "🚀 별 헤는 밤 백엔드 배포 시작... ($(date))"
echo "📝 로그: $LOG_FILE"

# ===== 공통 함수 =====
log_step() {
  echo ""
  echo "═══════════════════════════════════════════════════════════"
  echo "$1"
  echo "═══════════════════════════════════════════════════════════"
}

check_command() {
  if ! command -v "$1" &> /dev/null; then
    echo "❌ 필수 명령어 없음: $1"
    exit 1
  fi
}

# ===== 0. 환경 검증 =====
log_step "0️⃣ 환경 검증"
check_command docker
check_command git
check_command jq
check_command curl

if [ -z "${GITHUB_USERNAME:-}" ] || [ -z "${GITHUB_TOKEN:-}" ]; then
  echo "❌ GITHUB_USERNAME 또는 GITHUB_TOKEN 환경변수 없음"
  exit 1
fi

echo "✅ 환경 검증 완료"

# ===== 1. 기존 서비스 정리 =====
log_step "1️⃣ 기존 서비스 정리"
echo "🗑️ Docker 로그 초기화..."
for container in $(docker compose ps -q 2>/dev/null); do
  LOG_PATH=$(docker inspect --format='{{.LogPath}}' "$container" 2>/dev/null || echo "")
  if [ -n "$LOG_PATH" ] && [ -f "$LOG_PATH" ]; then
    truncate -s 0 "$LOG_PATH" 2>/dev/null || true
    echo "   ✓ 로그 초기화: $(docker inspect --format='{{.Name}}' "$container" | sed 's/^\///')"
  fi
done

echo "🛑 Docker 컨테이너 중지..."
docker compose down --remove-orphans 2>/dev/null || true
sleep 2

echo "🧹 Gradle 데몬 정지..."
./gradlew --stop 2>/dev/null || true

echo "✅ 기존 서비스 정리 완료"

# ===== 2. 코드 업데이트 =====
log_step "2️⃣ 코드 업데이트"
echo "📥 메인 저장소 업데이트..."
git fetch origin main || { echo "❌ git fetch 실패"; exit 1; }
git reset --hard origin/main || { echo "❌ git reset 실패"; exit 1; }
chmod +x ./gradlew

echo "📦 Config 저장소 업데이트..."
if [ ! -d "configs" ]; then
  git clone -b main "https://${GITHUB_USERNAME}:${GITHUB_TOKEN}@github.com/Iusto/byeolnight-config.git" configs \
    || { echo "❌ Config 저장소 clone 실패"; exit 1; }
else
  cd configs || exit 1
  git fetch origin main || { echo "❌ Config fetch 실패"; cd ..; exit 1; }
  git reset --hard origin/main || { echo "❌ Config reset 실패"; cd ..; exit 1; }
  cd ..
fi

echo "✅ 코드 업데이트 완료"

# ===== 3. 빌드 정리 =====
log_step "3️⃣ 빌드 정리"
echo "🧹 이전 빌드 정리..."
./gradlew clean || true
echo "✅ 빌드 정리 완료"

# ===== 4. 애플리케이션 빌드 =====
log_step "4️⃣ 애플리케이션 빌드"
echo "🔨 bootJar 빌드 시작..."
chmod +x ./gradlew

./gradlew bootJar -x test --no-daemon -Dorg.gradle.vfs.watch=false || {
  echo "❌ 빌드 실패"
  exit 1
}

if [ ! -f "build/libs/"*.jar ]; then
  echo "❌ JAR 파일 생성 실패"
  exit 1
fi

echo "✅ 빌드 완료: $(ls -lh build/libs/*.jar | awk '{print $9, $5}')"

# ===== 5. Config Server 빌드 및 기동 =====
log_step "5️⃣ Config Server 빌드 및 기동"
echo "🔨 Config Server Gradle 빌드..."
cd config-server || { echo "❌ config-server 디렉터리 이동 실패"; exit 1; }
chmod +x ./gradlew
./gradlew bootJar -x test --no-daemon || { echo "❌ Config Server Gradle 빌드 실패"; exit 1; }
cd ..

echo "🐳 Config Server Docker 이미지 빌드..."
docker compose build config-server || { echo "❌ Config Server Docker 빌드 실패"; exit 1; }

echo "⚙️ Config Server 시작..."
docker compose up -d config-server || { echo "❌ Config Server 시작 실패"; exit 1; }

echo "⏳ Config Server 헬스체크 (최대 60초)..."
CONFIG_READY=false
for i in $(seq 1 30); do
  if docker exec byeolnight-config-server-1 curl -s http://localhost:8888/actuator/health >/dev/null 2>&1; then
    echo "✅ Config Server 준비 완료 (${i}초)"
    CONFIG_READY=true
    break
  fi
  echo "⌛ 대기중... ($i/30)"
  sleep 2
done

if [ "$CONFIG_READY" = false ]; then
  echo "❌ Config Server 준비 시간 초과"
  docker logs byeolnight-config-server-1 2>&1 | tail -50
  exit 1
fi

# ===== 6. 설정값 로드 =====
log_step "6️⃣ 설정값 로드"
echo "🔑 Config Server에서 설정 가져오기..."

CONFIG_RESPONSE=""
for attempt in $(seq 1 5); do
  echo "시도 $attempt/5..."
  CONFIG_RESPONSE=$(docker exec byeolnight-config-server-1 curl -s -f http://localhost:8888/byeolnight/prod 2>/dev/null || echo "")
  
  if [[ -n "$CONFIG_RESPONSE" ]] && echo "$CONFIG_RESPONSE" | jq empty 2>/dev/null; then
    echo "✅ Config 응답 수신"
    break
  fi
  
  if [ $attempt -eq 5 ]; then
    echo "❌ Config Server 응답 실패"
    docker logs byeolnight-config-server-1 2>&1 | tail -30
    exit 1
  fi
  sleep 3
done

# 설정값 추출
MYSQL_ROOT_PASSWORD=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source."docker.mysql.root-password"' 2>/dev/null)
REDIS_PASSWORD=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source."docker.redis.password"' 2>/dev/null)

# 검증
if [[ -z "$MYSQL_ROOT_PASSWORD" || "$MYSQL_ROOT_PASSWORD" == "null" ]]; then
  echo "❌ MYSQL_ROOT_PASSWORD 추출 실패"
  echo "🔍 Config 응답:"
  echo "$CONFIG_RESPONSE" | jq '.propertySources[0].source | with_entries(select(.key | startswith("docker")))' 2>/dev/null
  exit 1
fi

if [[ -z "$REDIS_PASSWORD" || "$REDIS_PASSWORD" == "null" ]]; then
  echo "❌ REDIS_PASSWORD 추출 실패"
  exit 1
fi

echo "✅ 설정값 검증 완료"
echo "   - MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:0:3}***"
echo "   - REDIS_PASSWORD: ${REDIS_PASSWORD:0:3}***"

# 환경변수 내보내기 (Docker Compose에서 사용)
export MYSQL_ROOT_PASSWORD REDIS_PASSWORD

# ===== 7. 백엔드 서비스 배포 =====
log_step "7️⃣ 백엔드 서비스 배포"
echo "🐳 Docker 이미지 빌드..."
docker compose build app || { echo "❌ 이미지 빌드 실패"; exit 1; }

echo "🚀 백엔드 서비스 시작..."
docker compose up -d app || { echo "❌ 서비스 시작 실패"; exit 1; }

echo "⏳ 애플리케이션 헬스체크 (최대 120초)..."
APP_READY=false
for i in $(seq 1 60); do
  if curl -s -f http://localhost:8080/actuator/health >/dev/null 2>&1; then
    echo "✅ 애플리케이션 준비 완료 (${i}초)"
    APP_READY=true
    break
  fi
  echo "⌛ 대기중... ($i/60)"
  sleep 2
done

if [ "$APP_READY" = false ]; then
  echo "⚠️ 애플리케이션 헬스체크 시간 초과 (백그라운드에서 계속 시작 중)"
  echo "📋 최근 로그:"
  docker logs --tail 50 byeolnight-app-1 2>&1
fi

echo "🛑 배포가 완료되었으므로 Config Server 중지..."
docker compose stop config-server || echo "⚠️ Config Server 중지 실패(이미 꺼져있을 수 있음)"
echo "✅ Config Server 중지 완료"

log_step "✅ 배포 완료"
echo "🎉 별 헤는 밤 백엔드 배포 성공!"
echo "📝 프론트엔드는 S3+CloudFront에서 자동 배포됩니다."
echo ""
echo "📊 서비스 상태:"
docker compose ps
echo ""
echo "📋 실시간 로그 확인:"
echo "   docker logs -f byeolnight-app-1"
echo ""
echo "🔍 헬스체크:"
echo "   curl http://localhost:8080/actuator/health"
echo ""
echo "📝 배포 로그: $LOG_FILE"