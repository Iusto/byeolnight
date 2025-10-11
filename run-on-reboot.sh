#!/usr/bin/env bash
set -euo pipefail
export TZ=Asia/Seoul
timestamp(){ date '+%F %T %Z'; }

APPDIR="/home/ubuntu/byeolnight"
LOG="/home/ubuntu/byeolnight-startup.log"
TMP_ERR="$(mktemp)"
trap 'rm -f "$TMP_ERR" "$TMP_ERR".*' EXIT

echo "$(timestamp) ENTER run-on-reboot.sh" >> "$LOG"
cd "$APPDIR"

# 1) 배포 실행 (배포 스크립트 오류는 기록만, 판정은 헬스체크로)
if ! /bin/bash ./deploy.sh >/dev/null 2>"$TMP_ERR"; then
  echo "$(timestamp) 배포경고: deploy.sh 비정상 종료코드. 헬스체크로 최종판정 예정" >> "$LOG"
fi

# 2) 헬스체크 (필요에 맞게 수정: 포트/URL)
HEALTH_URL="http://127.0.0.1:8080/actuator/health"
# 준비시간 대기(최대 60초 예시)
for i in {1..30}; do
  if curl -fsS "$HEALTH_URL" | grep -q '"status":"UP"'; then
    echo "$(timestamp) 성공적으로 실행" >> "$LOG"
    exit 0
  fi
  sleep 2
done

# 3) 실패 처리(에러요약 포함)
REASON="$(tail -n 50 "$TMP_ERR" | tr '\n' ' ' | sed 's/[[:space:]]\+/ /g')"
echo "$(timestamp) 실패: 헬스체크 실패. 상세: $REASON" >> "$LOG"
exit 1
