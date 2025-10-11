# /home/ubuntu/byeolnight/run-on-reboot.sh
#!/usr/bin/env bash
set -euo pipefail
export TZ=Asia/Seoul

timestamp(){ date '+%F %T %Z'; }

APPDIR="/home/ubuntu/byeolnight"
LOG="/home/ubuntu/byeolnight-startup.log"
TMP_ERR="$(mktemp)"; trap 'rm -f "$TMP_ERR"' EXIT

cd "$APPDIR"

# 실행 비트 문제/ noexec 대비해 bash로 직접 실행
if /bin/bash ./deploy.sh >/dev/null 2>"$TMP_ERR"; then
  # 성공 시 아무것도 기록하지 않음
  :
else
  REASON="$(tail -n 50 "$TMP_ERR" | tr '\n' ' ' | sed 's/[[:space:]]\+/ /g')"
  echo "$(timestamp) 실패: $REASON" >> "$LOG"
  exit 1
fi
