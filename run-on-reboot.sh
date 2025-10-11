# /home/ubuntu/byeolnight/run-on-reboot.sh
#!/usr/bin/env bash
set -euo pipefail

APPDIR="/home/ubuntu/byeolnight"
LOG="/home/ubuntu/byeolnight-startup.log"
TMP_ERR="$(mktemp)"

timestamp() { date '+%F %T'; }

# 작업 디렉토리 진입 실패도 사유로 기록
if ! cd "$APPDIR" 2>/dev/null; then
  echo "$(timestamp) 실패: 디렉터리 진입 실패 ($APPDIR)" >> "$LOG"
  exit 1
fi

chmod +x ./deploy.sh || true

# 표준출력은 버리고, 표준에러만 임시 파일에 모음
if ./deploy.sh >/dev/null 2>"$TMP_ERR"; then
  echo "$(timestamp) 성공적으로 실행" >> "$LOG"
else
  # 에러가 길면 마지막 50줄만 요약 저장
  REASON="$(tail -n 50 "$TMP_ERR")"
  # 줄바꿈을 공백으로 정리(선택)
  REASON_ONE_LINE="$(echo "$REASON" | tr '\n' ' ' | sed 's/[[:space:]]\+/ /g')"
  echo "$(timestamp) 실패: $REASON_ONE_LINE" >> "$LOG"
  exit 1
fi

rm -f "$TMP_ERR"
