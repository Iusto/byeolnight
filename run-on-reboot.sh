# /home/ubuntu/byeolnight/run-on-reboot.sh
#!/usr/bin/env bash
set -euo pipefail

# 스크립트 전체 KST
export TZ=Asia/Seoul

# 로그 타임스탬프 (타임존 표기 포함)
timestamp() { date '+%F %T %Z'; }

APPDIR="/home/ubuntu/byeolnight"
LOG="/home/ubuntu/byeolnight-startup.log"
TMP_ERR="$(mktemp)"

# 종료 시 임시파일 정리
trap 'rm -f "$TMP_ERR"' EXIT

# 작업 디렉토리 진입 실패도 사유로 기록
if ! cd "$APPDIR" 2>/dev/null; then
  echo "$(timestamp) 실패: 디렉터리 진입 실패 ($APPDIR)" >> "$LOG"
  exit 1
fi

chmod +x ./deploy.sh

# 표준출력은 버리고, 표준에러만 임시 파일에 모음
if ./deploy.sh >/dev/null 2>"$TMP_ERR"; then
  echo "$(timestamp) 성공적으로 실행" >> "$LOG"
else
  REASON_ONE_LINE="$(tail -n 50 "$TMP_ERR" | tr '\n' ' ' | sed 's/[[:space:]]\+/ /g')"
  echo "$(timestamp) 실패: $REASON_ONE_LINE" >> "$LOG"
  exit 1
fi
