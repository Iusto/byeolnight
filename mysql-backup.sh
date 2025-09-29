#!/usr/bin/env bash

set -Eeo pipefail

### 설정 ###
CONTAINER="byeolnight-mysql-1"                 # MySQL 컨테이너 이름
BACKUP_DIR="/home/ubuntu/mysql-backups"        # 절대경로 고정(중요)
LOG_FILE="$BACKUP_DIR/backup.log"
RETAIN_DAYS=7                                  # 보존 일수
LOCK_FILE="/var/lock/mysql-backup.lock"        # flock 잠금 파일
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"

### 준비 ###
mkdir -p "$BACKUP_DIR"
touch "$LOG_FILE"
# 잠금(중복 실행 방지). 실패시 즉시 종료.
exec 200>"$LOCK_FILE"
flock -n 200 || { echo "[$TIMESTAMP] 이미 백업이 실행 중입니다." | tee -a "$LOG_FILE"; exit 1; }

log() { echo "[$(date +%Y%m%d_%H%M%S)] $*" | tee -a "$LOG_FILE"; }

log "MySQL 백업 시작..."

# 컨테이너 확인
if ! docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
  log "오류: 컨테이너 '$CONTAINER' 가 실행 중이 아닙니다."
  exit 2
fi

# 비밀번호 획득 (컨테이너 환경변수)
MYSQL_PASSWORD="$(docker exec "$CONTAINER" printenv MYSQL_ROOT_PASSWORD 2>/dev/null || true)"

# 파일 경로
LOGICAL_FILE="$BACKUP_DIR/logical_${TIMESTAMP}.sql.gz"
PHYSICAL_FILE="$BACKUP_DIR/physical_${TIMESTAMP}.tar.gz"
LOGICAL_SHA="$LOGICAL_FILE.sha256"
PHYSICAL_SHA="$PHYSICAL_FILE.sha256"

### 1) 논리 백업 (mysqldump)
log "논리적 백업 생성 중..."
# 경고 숨기기 위해 컨테이너 내부에서 MYSQL_PWD 사용
docker exec -e MYSQL_PWD="$MYSQL_PASSWORD" "$CONTAINER" \
  sh -c 'mysqldump -u root --all-databases --routines --triggers --events --single-transaction --quick' \
  | gzip > "$LOGICAL_FILE"

# 무결성 점검 & 해시
gzip -t "$LOGICAL_FILE"
sha256sum "$LOGICAL_FILE" > "$LOGICAL_SHA"

### 2) 물리 백업 (데이터 디렉터리 tar)
log "물리적 백업 생성 중..."
TMP_TAR="/tmp/mysql_data_${TIMESTAMP}.tar.gz"
docker exec "$CONTAINER" sh -c "tar -C /var/lib/mysql -czf '$TMP_TAR' ."
docker cp "$CONTAINER:$TMP_TAR" "$PHYSICAL_FILE"
docker exec "$CONTAINER" rm -f "$TMP_TAR"

# 무결성 점검 & 해시
tar -tzf "$PHYSICAL_FILE" >/dev/null
sha256sum "$PHYSICAL_FILE" > "$PHYSICAL_SHA"

### 부가 정보
LOGICAL_SIZE="$(du -h "$LOGICAL_FILE" | awk '{print $1}')"
PHYSICAL_SIZE="$(du -h "$PHYSICAL_FILE" | awk '{print $1}')"
MYSQL_VERSION="$(docker exec "$CONTAINER" mysql --version || echo 'unknown')"

log "백업 완료"
log "- 논리적 백업: $(basename "$LOGICAL_FILE") ($LOGICAL_SIZE)"
log "- 물리적 백업: $(basename "$PHYSICAL_FILE") ($PHYSICAL_SIZE)"
log "- MySQL 버전: $MYSQL_VERSION"

### 보존 정책
log "보존 정책: ${RETAIN_DAYS}일 지난 파일 정리"
find "$BACKUP_DIR" -type f \( -name 'logical_*.sql.gz' -o -name 'physical_*.tar.gz' -o -name '*.sha256' \) -mtime +"$RETAIN_DAYS" -print -delete | sed 's/^/[삭제]/' | tee -a "$LOG_FILE" || true

log "백업이 완료되었습니다: $BACKUP_DIR"
echo "" >> "$LOG_FILE"
