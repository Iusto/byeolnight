#!/usr/bin/env bash
# /home/ubuntu/byeolnight/run-on-reboot.sh
# EC2 재부팅 시 자동 배포 스크립트
set -Eeuo pipefail
export TZ=Asia/Seoul

timestamp() { date '+%Y-%m-%d %H:%M:%S %Z'; }

APPDIR="/home/ubuntu/byeolnight"
LOG="$APPDIR/logs/startup.log"
MAX_LOG_SIZE=10485760  # 10MB

mkdir -p "$APPDIR/logs"

# 로그 파일 크기 제한
if [ -f "$LOG" ] && [ $(stat -f%z "$LOG" 2>/dev/null || stat -c%s "$LOG" 2>/dev/null || echo 0) -gt $MAX_LOG_SIZE ]; then
  mv "$LOG" "${LOG}.old"
  echo "$(timestamp) 로그 파일 로테이션" > "$LOG"
fi

log_msg() {
  echo "$(timestamp) $1" | tee -a "$LOG"
}

log_msg "═══════════════════════════════════════════════════════════"
log_msg "🔄 재부팅 후 자동 배포 시작"
log_msg "═══════════════════════════════════════════════════════════"

# 환경변수 확인
if [ -z "${GITHUB_USERNAME:-}" ] || [ -z "${GITHUB_TOKEN:-}" ]; then
  log_msg "❌ GITHUB_USERNAME 또는 GITHUB_TOKEN 환경변수 없음"
  log_msg "📝 /etc/environment 또는 crontab에 환경변수를 설정하세요"
  exit 1
fi

# 디렉터리 이동
if ! cd "$APPDIR" 2>>"$LOG"; then
  log_msg "❌ 디렉터리 이동 실패: $APPDIR"
  exit 1
fi

# Docker 서비스 대기
log_msg "⏳ Docker 서비스 대기 중..."
for i in $(seq 1 30); do
  if docker info >/dev/null 2>&1; then
    log_msg "✅ Docker 서비스 준비 완료"
    break
  fi
  if [ $i -eq 30 ]; then
    log_msg "❌ Docker 서비스 시간 초과"
    exit 1
  fi
  sleep 2
done

# 배포 실행
log_msg "🚀 배포 스크립트 실행..."
if /bin/bash ./deploy.sh >> "$LOG" 2>&1; then
  log_msg "✅ 재부팅 후 배포 성공"
  exit 0
else
  log_msg "❌ 재부팅 후 배포 실패"
  log_msg "📋 상세 로그는 위 출력을 확인하세요"
  exit 1
fi
