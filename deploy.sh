#!/bin/bash
# EC2 서버 원클릭 배포 스크립트
# 사용법: chmod +x deploy.sh && ./deploy.sh
set -euo pipefail

echo "🚀 별 헤는 밤 배포 시작..."

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

# ===== 0. 포트/프로세스 충돌 방지(과격 종료 제거) =====
# 무조건 pkill nginx는 위험하므로 제거. Docker nginx는 compose로만 제어.
echo "🔧 기존 컨테이너 정리..."
docker compose down --remove-orphans || true

# 혹시 이전 배포에서 호스트에 떠있는 Java/Gradle가 build/를 잡고 있을 수 있음
./gradlew --stop || true

# ===== 1. 코드 업데이트 =====
echo "📥 최신 코드 가져오기..."
git fetch origin master && git reset --hard origin/master

# ===== 2. Gradle 클린(안전 가드 포함) =====
echo "🧽 Gradle 클린 시작..."
kill_holders
# 1차 시도: 데몬/파일워처 끄고 clean
if ! ./gradlew clean --no-daemon -Dorg.gradle.vfs.watch=false; then
  echo "⚠️ gradlew clean 실패 → 홀더 재정리 후 재시도"
  kill_holders
  hard_clean_build
  # 2차 시도
  ./gradlew clean --no-daemon -Dorg.gradle.vfs.watch=false || true
fi

# 그래도 남았을 가능성 방지
hard_clean_build

# ===== 3. 서버 빌드 =====
echo "🔨 bootJar 빌드..."
chmod +x ./gradlew
./gradlew bootJar -x test --no-daemon -Dorg.gradle.vfs.watch=false

# ===== 4. Config Server 기동 =====
echo "⚙️ Config Server 시작..."
docker compose up config-server -d
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

# ===== 5. 비밀값 로드 =====
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

if [[ -z "$MYSQL_ROOT_PASSWORD" || -z "$REDIS_PASSWORD" || "$MYSQL_ROOT_PASSWORD" == "null" || "$REDIS_PASSWORD" == "null" ]]; then
  echo "❌ 비밀번호 추출 실패"
  exit 1
fi

echo "환경변수 확인: MYSQL_ROOT_PASSWORD=$(echo "$MYSQL_ROOT_PASSWORD" | cut -c1-3)***  REDIS_PASSWORD=$(echo "$REDIS_PASSWORD" | cut -c1-3)***"
export MYSQL_ROOT_PASSWORD REDIS_PASSWORD

# Docker Compose용 .env 생성
cat > .env <<EOF
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
REDIS_PASSWORD=${REDIS_PASSWORD}
EOF

# ===== 6. 이미지 빌드 & 기동 =====
echo "🏗️ 서비스 빌드 및 배포..."
docker compose build --no-cache
docker compose up -d

# ===== 7. SSL 인증서 점검 & 갱신 =====
echo "🔒 SSL 인증서 갱신(webroot)"
# ACME 경로 사전 점검
sudo mkdir -p /var/www/certbot/.well-known/acme-challenge
echo OK | sudo tee /var/www/certbot/.well-known/acme-challenge/ping.txt >/dev/null
curl -sfI http://byeolnight.com/.well-known/acme-challenge/ping.txt >/dev/null || {
  echo "❌ ACME 경로 노출 실패(nginx.conf/볼륨 확인 필요)"; exit 1; }

# nginx는 그대로 둔 채 renew 실행
docker compose run --rm certbot renew || { echo "❌ renew 실패"; exit 1; }
docker compose exec -T nginx nginx -s reload || true
echo "✅ SSL 갱신 완료"

echo "✅ 배포 완료! 로그 출력..."
docker logs -f byeolnight-app-1
