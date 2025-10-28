#!/bin/bash
# EC2 백엔드 배포 스크립트 (프론트엔드는 S3+CloudFront)
# 사용법: chmod +x deploy.sh && ./deploy.sh
set -euo pipefail

LOG_FILE="/home/ubuntu/deploy-$(date +%Y%m%d-%H%M%S).log"
exec 1> >(tee -a "$LOG_FILE")
exec 2>&1

echo "🚀 별 헤는 밤 백엔드 배포 시작... ($(date))"
echo "📝 로그: $LOG_FILE"

ROOT_DIR="/home/ubuntu/byeolnight"
cd "$ROOT_DIR" || { echo "❌ 디렉터리 이동 실패"; exit 1; }

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

kill_holders() {
  if [ ! -d "./build" ]; then
    return 0
  fi
  
  echo "🔧 build/ 디렉터리 점유 프로세스 정리..."
  local pids
  pids=$(lsof -t +D ./build 2>/dev/null || true)
  
  if [[ -n "${pids:-}" ]]; then
    echo "⚠️ 점유 PID: $pids"
    kill $pids 2>/dev/null || true
    sleep 3
    
    pids=$(lsof -t +D ./build 2>/dev/null || true)
    if [[ -n "${pids:-}" ]]; then
      echo "⛔ 강제 종료: $pids"
      kill -9 $pids 2>/dev/null || true
      sleep 2
    fi
  fi
  
  fuser -k ./build 2>/dev/null || true
  echo "✅ 프로세스 정리 완료"
}

hard_clean_build() {
  echo "🧹 build/ 강제 정리..."
  if [ -d "./build" ]; then
    sudo chown -R ubuntu:ubuntu ./build 2>/dev/null || true
    chmod -R u+rwX ./build 2>/dev/null || true
    rm -rf ./build || { echo "⚠️ build/ 삭제 실패 (무시)"; }
  fi
  echo "✅ 빌드 디렉터리 정리 완료"
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
echo "🛑 Docker 컨테이너 중지..."
docker compose down --remove-orphans 2>/dev/null || true
sleep 2

echo "🧹 Gradle 데몬 정지..."
chmod +x ./gradlew 2>/dev/null || true
command -v dos2unix >/dev/null 2>&1 && dos2unix ./gradlew 2>/dev/null || true
./gradlew --stop 2>/dev/null || true

echo "✅ 기존 서비스 정리 완료"

# ===== 2. 코드 업데이트 =====
log_step "2️⃣ 코드 업데이트"
echo "📥 메인 저장소 업데이트..."
git fetch origin main || { echo "❌ git fetch 실패"; exit 1; }
git reset --hard origin/main || { echo "❌ git reset 실패"; exit 1; }

# gradlew 권한 복구
chmod +x ./gradlew 2>/dev/null || true
command -v dos2unix >/dev/null 2>&1 && dos2unix ./gradlew 2>/dev/null || true

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
kill_holders
hard_clean_build

echo "🧽 Gradle clean 실행..."
./gradlew clean --no-daemon -Dorg.gradle.vfs.watch=false 2>&1 || {
  echo "⚠️ Gradle clean 실패, 재시도..."
  sleep 2
  hard_clean_build
  ./gradlew clean --no-daemon -Dorg.gradle.vfs.watch=false 2>&1 || true
}

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

# ===== 5. Config Server 기동 =====
log_step "5️⃣ Config Server 기동"
echo "⚙️ Config Server 시작..."
docker compose up -d config-server || { echo "❌ Config Server 시작 실패"; exit 1; }

echo "⏳ Config Server 준비 대기 (최대 90초)..."
CONFIG_READY=false
for i in $(seq 1 45); do
  if curl -s -u config-admin:config-secret-2024 http://localhost:8888/actuator/health >/dev/null 2>&1; then
    echo "✅ Config Server 준비 완료 (${i}초)"
    CONFIG_READY=true
    break
  fi
  echo "⌛ Config Server 헬스체크 대기중... ($i/45)"
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
  CONFIG_RESPONSE=$(curl -s -f -u config-admin:config-secret-2024 \
    http://localhost:8888/byeolnight/prod 2>/dev/null || echo "")
  
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
MYSQL_ROOT_PASSWORD=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source["docker.mysql.root-password"]' 2>/dev/null || echo "")
REDIS_PASSWORD=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source["docker.redis.password"]' 2>/dev/null || echo "")
CONFIG_USERNAME=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source["config.server.username"]' 2>/dev/null || echo "")
CONFIG_PASSWORD=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source["config.server.password"]' 2>/dev/null || echo "")
CONFIG_ENCRYPT_KEY=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source["config.server.encrypt-key"]' 2>/dev/null || echo "")

# 검증
if [[ -z "$MYSQL_ROOT_PASSWORD" || "$MYSQL_ROOT_PASSWORD" == "null" ]]; then
  echo "❌ MYSQL_ROOT_PASSWORD 추출 실패"
  exit 1
fi

if [[ -z "$REDIS_PASSWORD" || "$REDIS_PASSWORD" == "null" ]]; then
  echo "❌ REDIS_PASSWORD 추출 실패"
  exit 1
fi

if [[ -z "$CONFIG_USERNAME" || -z "$CONFIG_PASSWORD" || -z "$CONFIG_ENCRYPT_KEY" ]]; then
  echo "❌ Config Server 자격증명 추출 실패"
  exit 1
fi

echo "✅ 설정값 검증 완료"
echo "   - MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:0:3}***"
echo "   - REDIS_PASSWORD: ${REDIS_PASSWORD:0:3}***"
echo "   - CONFIG_USERNAME: $CONFIG_USERNAME"

# 환경변수 내보내기 (Docker Compose에서 사용)
export MYSQL_ROOT_PASSWORD REDIS_PASSWORD CONFIG_USERNAME CONFIG_PASSWORD CONFIG_ENCRYPT_KEY

# ===== 7. 백엔드 서비스 배포 =====
log_step "7️⃣ 백엔드 서비스 배포"
echo "🐳 Docker 이미지 빌드..."
docker compose build --no-cache app || { echo "❌ 이미지 빌드 실패"; exit 1; }

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
  echo "⌛ 애플리케이션 시작 대기중... ($i/60)"
  sleep 2
done

if [ "$APP_READY" = false ]; then
  echo "⚠️ 애플리케이션 헬스체크 시간 초과 (백그라운드에서 계속 시작 중)"
  echo "📋 최근 로그:"
  docker logs --tail 50 byeolnight-app-1 2>&1
fi

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
