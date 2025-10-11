# /home/ubuntu/byeolnight/run-on-reboot.sh
#!/usr/bin/env bash
set -Eeuo pipefail
export TZ=Asia/Seoul

timestamp(){ date '+%F %T %Z'; }

APPDIR="/home/ubuntu/byeolnight"
LOG="/home/ubuntu/byeolnight-startup.log"
TMP_ERR="$(mktemp)"

cleanup() { rm -f "$TMP_ERR"; }
on_err() {
  local line="${1:-?}"
  # stderr 모아둔 내용 요약
  local reason="$(tail -n 50 "$TMP_ERR" 2>/dev/null | tr '\n' ' ' | sed 's/[[:space:]]\+/ /g')"
  [[ -z "$reason" ]] && reason="unknown error (line $line)"
  echo "$(timestamp) 실패: $reason" >> "$LOG"
  cleanup
  exit 1
}

trap 'on_err $LINENO' ERR
trap cleanup EXIT

cd "$APPDIR" 2>>"$TMP_ERR"

# 배포 실행(표준출력 버리고, 표준에러만 수집)
# noexec/권한 이슈 회피를 위해 bash로 실행
/bin/bash ./deploy.sh >/dev/null 2>>"$TMP_ERR"

# 여기까지 도달하면 성공 → 아무 것도 기록하지 않음
exit 0
