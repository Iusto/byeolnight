#!/bin/bash
# MySQL 백업 스크립트
# 사용법: ./mysql-backup.sh

set -e

# 설정
BACKUP_DIR="$HOME/mysql-backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="$BACKUP_DIR/backup.log"

# 백업 디렉토리 생성
mkdir -p "$BACKUP_DIR"

echo "[${TIMESTAMP}] MySQL 백업 시작..." | tee -a "$LOG_FILE"

# Docker 컨테이너에서 MySQL 비밀번호 가져오기
MYSQL_PASSWORD=$(docker exec byeolnight-mysql-1 printenv MYSQL_ROOT_PASSWORD 2>/dev/null || echo "byeolnight9703!@")

# 1. 논리적 백업 (mysqldump)
echo "논리적 백업 생성 중..." | tee -a "$LOG_FILE"
docker exec byeolnight-mysql-1 mysqldump -u root -p"$MYSQL_PASSWORD" --all-databases --routines --triggers --events --single-transaction | gzip > "$BACKUP_DIR/logical_${TIMESTAMP}.sql.gz"

# 2. 물리적 백업 (데이터 디렉토리)
echo "물리적 백업 생성 중..." | tee -a "$LOG_FILE"
docker exec byeolnight-mysql-1 tar -czf /tmp/mysql_data_${TIMESTAMP}.tar.gz -C /var/lib/mysql .
docker cp byeolnight-mysql-1:/tmp/mysql_data_${TIMESTAMP}.tar.gz "$BACKUP_DIR/physical_${TIMESTAMP}.tar.gz"
docker exec byeolnight-mysql-1 rm /tmp/mysql_data_${TIMESTAMP}.tar.gz

# 파일 크기 확인
LOGICAL_SIZE=$(du -h "$BACKUP_DIR/logical_${TIMESTAMP}.sql.gz" | cut -f1)
PHYSICAL_SIZE=$(du -h "$BACKUP_DIR/physical_${TIMESTAMP}.tar.gz" | cut -f1)

# MySQL 버전 확인
MYSQL_VERSION=$(docker exec byeolnight-mysql-1 mysql --version)

# 로그 기록
echo "[${TIMESTAMP}] 백업 완료" | tee -a "$LOG_FILE"
echo "- 논리적 백업: logical_${TIMESTAMP}.sql.gz (${LOGICAL_SIZE})" | tee -a "$LOG_FILE"
echo "- 물리적 백업: physical_${TIMESTAMP}.tar.gz (${PHYSICAL_SIZE})" | tee -a "$LOG_FILE"
echo "- MySQL 버전: ${MYSQL_VERSION}" | tee -a "$LOG_FILE"
echo "" | tee -a "$LOG_FILE"

# 7일 이상 된 백업 파일 삭제
find "$BACKUP_DIR" -name "*.sql.gz" -mtime +7 -delete
find "$BACKUP_DIR" -name "*.tar.gz" -mtime +7 -delete

echo "백업이 완료되었습니다: $BACKUP_DIR"