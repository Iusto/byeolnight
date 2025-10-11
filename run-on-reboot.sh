#!/usr/bin/env bash
set -euo pipefail
export TZ=Asia/Seoul
timestamp() { date '+%F %T %Z'; }

APPDIR="/home/ubuntu/byeolnight"
LOG="/home/ubuntu/byeolnight-startup.log"
TMP_ERR="$(mktemp)"
trap 'rm -f "$TMP_ERR" "$TMP_ERR".*' EXIT

# 디버그(임시): 진입 흔적 남기기
echo "$(timestamp) ENTER run-on-reboot.sh" >> "$LOG"

# 1) 작업 디렉토리 진입
if ! cd "$APPDIR" 2>"$TMP_ERR.cd"; then
  echo "$(timestamp) 실패: 디렉터리 진입 실패 ($APPDIR) - $(tr '\n' ' ' < "$TMP_ERR.cd")" >> "$LOG"
  exit 1
fi

# 2) deploy.sh 권한 정리 (실패도 기록)
if ! chmod 755 ./deploy.sh 2>"$TMP_ERR.chmod"; then
  echo "$(timestamp) 실패: chmod 755 ./deploy.sh - $(tr '\n' ' ' < "$TMP_ERR.chmod")" >> "$LOG"
  exit 1
fi

# 3) 실행 (bash로 직접 실행: noexec/실행비트 이슈 회피)
if /bin/bash ./deploy.sh >/dev/null 2>"$TMP_ERR"; then
  echo "$(timestamp) 성공적으로 실행" >> "$LOG"
else
  REASON_ONE_LINE="$(tail -n 50 "$TMP_ERR" | tr '\n' ' ' | sed 's/[[:space:]]\+/ /g')"
  echo "$(timestamp) 실패: $REASON_ONE_LINE" >> "$LOG"
  exit 1
fi
