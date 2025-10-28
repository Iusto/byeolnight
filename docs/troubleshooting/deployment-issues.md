# 🚀 자동배포 트러블슈팅 가이드

## 📋 목차
1. [배포 전 체크리스트](#배포-전-체크리스트)
2. [일반적인 문제와 해결방법](#일반적인-문제와-해결방법)
3. [로그 확인 방법](#로그-확인-방법)
4. [수동 복구 절차](#수동-복구-절차)

---

## 배포 전 체크리스트

### EC2 환경변수 설정
```bash
# /etc/environment 또는 ~/.bashrc에 추가
export GITHUB_USERNAME="your-username"
export GITHUB_TOKEN="ghp_xxxxxxxxxxxxx"
```

### GitHub Secrets 설정
- `EC2_HOST`: EC2 퍼블릭 IP 또는 도메인
- `DEPLOY_SSH_KEY`: EC2 접속용 SSH 개인키
- `GH_USERNAME`: GitHub 사용자명
- `GH_TOKEN`: GitHub Personal Access Token (repo 권한)

### 필수 명령어 확인
```bash
# EC2에서 실행
command -v docker && echo "✅ Docker OK" || echo "❌ Docker 없음"
command -v git && echo "✅ Git OK" || echo "❌ Git 없음"
command -v jq && echo "✅ jq OK" || echo "❌ jq 없음"
command -v curl && echo "✅ curl OK" || echo "❌ curl 없음"
```

---

## 일반적인 문제와 해결방법

### 1. Config Server 연결 실패

**증상:**
```
❌ Config Server 응답 실패
```

**원인:**
- Config Server가 시작되지 않음
- 암호화 키 설정 오류
- configs 디렉터리 권한 문제

**해결방법:**
```bash
# Config Server 로그 확인
docker logs byeolnight-config-server-1

# Config Server 재시작
docker compose restart config-server

# configs 디렉터리 권한 확인
ls -la configs/
chmod -R 755 configs/

# 수동으로 Config 테스트
curl -u config-admin:config-secret-2024 \
  http://localhost:8888/byeolnight/prod | jq .
```

### 2. 빌드 디렉터리 점유 오류

**증상:**
```
Directory '/home/ubuntu/byeolnight/build' is locked
```

**원인:**
- Gradle 데몬이 build 디렉터리를 점유 중
- 이전 빌드 프로세스가 종료되지 않음

**해결방법:**
```bash
# Gradle 데몬 강제 종료
./gradlew --stop

# build 디렉터리 점유 프로세스 확인 및 종료
lsof +D ./build
fuser -k ./build

# build 디렉터리 강제 삭제
sudo rm -rf ./build
```

### 3. Docker 컨테이너 시작 실패

**증상:**
```
❌ 서비스 시작 실패
```

**원인:**
- 포트 충돌 (8080, 8888, 3306, 6379)
- 환경변수 누락
- 이미지 빌드 실패

**해결방법:**
```bash
# 포트 사용 확인
sudo netstat -tulpn | grep -E '8080|8888|3306|6379'

# 기존 컨테이너 완전 정리
docker compose down --volumes --remove-orphans
docker system prune -af

# 환경변수 확인
cat .env

# 수동 빌드 및 시작
docker compose build --no-cache
docker compose up -d
```

### 4. MySQL 연결 실패

**증상:**
```
Communications link failure
```

**원인:**
- MySQL 컨테이너가 완전히 시작되지 않음
- 비밀번호 불일치
- 네트워크 문제

**해결방법:**
```bash
# MySQL 로그 확인
docker logs byeolnight-mysql-1

# MySQL 헬스체크
docker exec byeolnight-mysql-1 mysqladmin ping -h localhost

# MySQL 접속 테스트
docker exec -it byeolnight-mysql-1 mysql -uroot -p

# MySQL 재시작
docker compose restart mysql
sleep 10
docker compose restart app
```

### 5. Redis 연결 실패

**증상:**
```
Unable to connect to Redis
```

**원인:**
- Redis 비밀번호 불일치
- Redis 컨테이너 미실행

**해결방법:**
```bash
# Redis 로그 확인
docker logs byeolnight-redis-1

# Redis 접속 테스트
docker exec -it byeolnight-redis-1 redis-cli
AUTH your-redis-password
PING

# Redis 재시작
docker compose restart redis
```

### 6. Git 업데이트 실패

**증상:**
```
❌ git fetch 실패
```

**원인:**
- GitHub 인증 실패
- 네트워크 문제
- 로컬 변경사항 충돌

**해결방법:**
```bash
# GitHub 인증 확인
echo $GITHUB_USERNAME
echo $GITHUB_TOKEN | cut -c1-10

# 로컬 변경사항 강제 리셋
git fetch origin main
git reset --hard origin/main
git clean -fd

# Config 저장소 재클론
rm -rf configs
git clone -b main \
  "https://${GITHUB_USERNAME}:${GITHUB_TOKEN}@github.com/Iusto/byeolnight-config.git" \
  configs
```

### 7. JAR 파일 생성 실패

**증상:**
```
❌ JAR 파일 생성 실패
```

**원인:**
- 빌드 오류
- 의존성 다운로드 실패
- 메모리 부족

**해결방법:**
```bash
# 상세 빌드 로그 확인
./gradlew bootJar -x test --no-daemon --stacktrace

# Gradle 캐시 정리
./gradlew clean --no-daemon
rm -rf ~/.gradle/caches/

# 메모리 확인
free -h
df -h

# 스왑 메모리 추가 (필요시)
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

---

## 로그 확인 방법

### 배포 스크립트 로그
```bash
# 최신 배포 로그 확인
ls -lt /home/ubuntu/deploy-*.log | head -1
tail -f /home/ubuntu/deploy-$(date +%Y%m%d)*.log
```

### Docker 컨테이너 로그
```bash
# 전체 서비스 로그
docker compose logs

# 특정 서비스 로그
docker logs byeolnight-app-1 --tail 100 -f
docker logs byeolnight-config-server-1 --tail 100 -f
docker logs byeolnight-mysql-1 --tail 100 -f
docker logs byeolnight-redis-1 --tail 100 -f

# 에러만 필터링
docker logs byeolnight-app-1 2>&1 | grep -i error
```

### 재부팅 자동 실행 로그
```bash
# 재부팅 후 자동 배포 로그
tail -f /home/ubuntu/byeolnight-startup.log
```

### 시스템 로그
```bash
# 시스템 전체 로그
sudo journalctl -xe

# Docker 서비스 로그
sudo journalctl -u docker -f
```

---

## 수동 복구 절차

### 완전 초기화 및 재배포
```bash
#!/bin/bash
# 모든 것을 초기화하고 처음부터 배포

cd /home/ubuntu/byeolnight

# 1. 모든 컨테이너 중지 및 삭제
docker compose down --volumes --remove-orphans
docker system prune -af --volumes

# 2. 빌드 디렉터리 정리
./gradlew --stop || true
sudo rm -rf ./build
sudo rm -rf .gradle

# 3. 코드 완전 리셋
git fetch origin main
git reset --hard origin/main
git clean -fd

# 4. Config 저장소 재클론
rm -rf configs
git clone -b main \
  "https://${GITHUB_USERNAME}:${GITHUB_TOKEN}@github.com/Iusto/byeolnight-config.git" \
  configs

# 5. 배포 실행
chmod +x deploy.sh
./deploy.sh
```

### 롤백 (이전 버전으로 복구)
```bash
# 특정 커밋으로 롤백
cd /home/ubuntu/byeolnight
git log --oneline -10  # 최근 10개 커밋 확인
git reset --hard <commit-hash>
./deploy.sh
```

### 긴급 서비스 재시작
```bash
# 코드 변경 없이 서비스만 재시작
cd /home/ubuntu/byeolnight
docker compose restart app

# 또는 전체 재시작
docker compose restart
```

---

## 예방 조치

### 1. 정기 모니터링
```bash
# crontab에 추가
# 매 시간 헬스체크
0 * * * * curl -sf http://localhost:8080/actuator/health || echo "App down" | mail -s "Alert" admin@example.com
```

### 2. 디스크 공간 관리
```bash
# 오래된 Docker 이미지 정리 (주 1회)
docker image prune -af --filter "until=168h"

# 오래된 로그 정리 (월 1회)
find /home/ubuntu -name "deploy-*.log" -mtime +30 -delete
```

### 3. 백업
```bash
# MySQL 백업 (일 1회)
docker exec byeolnight-mysql-1 mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} byeolnight \
  > /home/ubuntu/backup/byeolnight-$(date +%Y%m%d).sql
```

---

## 도움이 필요한 경우

1. **로그 수집**: 위의 로그 확인 명령어로 모든 로그 수집
2. **환경 정보**: `docker compose ps`, `docker version`, `free -h`, `df -h` 실행 결과
3. **에러 메시지**: 정확한 에러 메시지와 발생 시점
4. **재현 단계**: 문제가 발생한 정확한 단계

이 정보들을 가지고 이슈를 생성하거나 팀에 문의하세요.
