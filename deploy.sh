#!/bin/bash
# EC2 백엔드 배포 스크립트 (프론트엔드는 S3+CloudFront)
# 사용법: chmod +x deploy.sh && ./deploy.sh
set -euo pipefail

echo "🚀 별 헤는 밤 백엔드 배포 시작..."

ROOT_DIR="/home/ubuntu/byeolnight"
cd "$ROOT_DIR"

# ===== 공통 함수 =====
kill_holders() {
  echo "🔧 build/ 디렉터리를 점유 중인 프로세스 탐지/정리..."
  # build/를 열고 있는 PID 나열
  local pids
  pids=$(lsof -t +D ./build 2>/dev/null || true)
  if [[ -n "${pids:-}" ]]; then
    echo "⚠️ 점유 PID: $pids"
    # 정상 종료 시도
    kill $pids 2>/dev/null || true
    sleep 2
    # 살아있으면 강제 종료
    pids=$(lsof -t +D ./build 2>/dev/null || true)
    if [[ -n "${pids:-}" ]]; then
      echo "⛔ 강제 종료: $pids"
      kill -9 $pids 2>/dev/null || true
    fi
  else
    echo "✅ build/ 점유 프로세스 없음"
  fi

  # 혹시 모를 파일 핸들 닫기
  fuser -vm ./build 2>/dev/null || true
  fuser -k ./build 2>/dev/null || true
}

hard_clean_build() {
  echo "🧹 build/ 강제 정리..."
  sudo chown -R ubuntu:ubuntu ./build 2>/dev/null || true
  chmod -R u+rwX ./build 2>/dev/null || true
  rm -rf ./build || true
}

# ===== 0. 포트/프로세스 충돌 방지 =====
echo "🔧 기존 컨테이너 정리..."
docker compose down --remove-orphans --volumes || true
docker container prune -f || true

# ⬇️ gradlew 실행권한/줄바꿈 보정 먼저
chmod +x ./gradlew 2>/dev/null || true
command -v dos2unix >/dev/null 2>&1 && dos2unix ./gradlew 2>/dev/null || true

# 그 다음에 데몬 정지
./gradlew --stop || true

# ===== 1. Config Repository 업데이트 (코드 업데이트 전에 먼저) =====
if [ ! -d "config-repo" ]; then
  echo "📦 Config Repository clone..."
  git clone -b main https://${GITHUB_USERNAME}:${GITHUB_TOKEN}@github.com/Iusto/byeolnight-config.git config-repo
else
  echo "🔄 Config Repository 업데이트..."
  cd config-repo && git checkout main && git pull origin main && cd ..
fi

# ===== 2. 코드 업데이트 =====
echo "📥 최신 코드 가져오기..."
git fetch origin main && git reset --hard origin/main

# ⬇️ reset 후에 반드시 다시 실행권한/줄바꿈 보정
chmod +x ./gradlew 2>/dev/null || true
command -v dos2unix >/dev/null 2>&1 && dos2unix ./gradlew 2>/dev/null || true

# ===== 3. Gradle 클린 =====
echo "🧽 Gradle 클린 시작..."
kill_holders
./gradlew clean --no-daemon -Dorg.gradle.vfs.watch=false \
  || sh ./gradlew clean --no-daemon -Dorg.gradle.vfs.watch=false || true

# 그래도 남았을 가능성 방지
hard_clean_build

# ===== 4. 서버 빌드 =====
echo "🔨 bootJar 빌드..."
chmod +x ./gradlew
./gradlew bootJar -x test --no-daemon -Dorg.gradle.vfs.watch=false

# ===== 5. Config Server 기동 =====
echo "⚙️ Config Server 시작..."
docker compose up -d config-server
echo "⏳ Config Server 준비 대기..."
for i in $(seq 1 15); do
  if curl -s -u config-admin:config-secret-2024 http://localhost:8888/actuator/health >/dev/null 2>&1; then
    if curl -s -X POST http://localhost:8888/encrypt -d "test" | grep -q "AQA"; then
      echo "✅ Config Server OK(암호화 확인)"
      break
    fi
    echo "⌛ 암호화 기능 대기중... ($i/15)"
  else
    echo "⌛ Config Server 대기중... ($i/15)"
  fi
  sleep 2
done

# ===== 6. 비밀값 로드 =====
echo "🔑 Config Server에서 비밀번호 가져오기..."
CONFIG_RESPONSE=""
for attempt in 1 2 3 4 5; do
  echo "시도 $attempt/5"
  CONFIG_RESPONSE=$(curl -s -u config-admin:config-secret-2024 http://localhost:8888/byeolnight/prod 2>/dev/null || echo "")
  if [[ -n "$CONFIG_RESPONSE" ]] && echo "$CONFIG_RESPONSE" | jq . >/dev/null 2>&1; then
    echo "✅ Config Server 응답 수신"
    break
  fi
  sleep 3
  [[ $attempt -eq 5 ]] && echo "❌ Config Server 연결 실패" && exit 1
done

MYSQL_ROOT_PASSWORD=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source["docker.mysql.root-password"]' 2>/dev/null || echo "")
REDIS_PASSWORD=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source["docker.redis.password"]' 2>/dev/null || echo "")
CONFIG_USERNAME=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source["config.server.username"]' 2>/dev/null || echo "")
CONFIG_PASSWORD=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source["config.server.password"]' 2>/dev/null || echo "")
CONFIG_ENCRYPT_KEY=$(echo "$CONFIG_RESPONSE" | jq -r '.propertySources[0].source["config.server.encrypt-key"]' 2>/dev/null || echo "")

if [[ -z "$MYSQL_ROOT_PASSWORD" || -z "$REDIS_PASSWORD" || "$MYSQL_ROOT_PASSWORD" == "null" || "$REDIS_PASSWORD" == "null" ]]; then
  echo "❌ 비밀번호 추출 실패"
  exit 1
fi

if [[ -z "$CONFIG_USERNAME" || -z "$CONFIG_PASSWORD" || -z "$CONFIG_ENCRYPT_KEY" ]]; then
  echo "❌ Config Server 자격증명 추출 실패"
  exit 1
fi

echo "환경변수 확인: MYSQL_ROOT_PASSWORD=$(echo "$MYSQL_ROOT_PASSWORD" | cut -c1-3)***  REDIS_PASSWORD=$(echo "$REDIS_PASSWORD" | cut -c1-3)***"
export MYSQL_ROOT_PASSWORD REDIS_PASSWORD CONFIG_USERNAME CONFIG_PASSWORD CONFIG_ENCRYPT_KEY

# Docker Compose용 .env 생성
cat > .env <<EOF
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
REDIS_PASSWORD=${REDIS_PASSWORD}
CONFIG_USERNAME=${CONFIG_USERNAME}
CONFIG_PASSWORD=${CONFIG_PASSWORD}
CONFIG_ENCRYPT_KEY=${CONFIG_ENCRYPT_KEY}
EOF

# ===== 7. 백엔드 서비스 기동 =====
echo "🏗️ 백엔드 서비스 배포..."
docker compose build --no-cache app
docker compose up -d app

echo "✅ 백엔드 배포 완료!"
echo "📝 프론트엔드는 S3+CloudFront에서 자동 배포됩니다."
echo ""
echo "로그 확인:"
docker logs -f byeolnight-app-1
