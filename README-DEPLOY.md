# 🚀 별 헤는 밤 (Byeolnight) 배포 가이드

## ⚡ 빠른 배포

### 🌐 **Docker 전체 배포 (프로덕션)**
```bash
# 전체 서비스 빌드 및 실행
docker-compose up --build -d

# 또는 배포 스크립트 사용 (Windows)
./deploy.bat
```

### 🏠 **로컬 개발 환경**
```bash
# 데이터베이스만 Docker로 실행
docker-compose -f docker-compose.local.yml up -d

# Spring Boot 애플리케이션 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# 프론트엔드 실행 (별도 터미널)
cd byeolnight-frontend
npm run dev
```

---

## 📋 배포 전 체크리스트

### 1. 필수 요구사항
- [ ] Java 21 이상 설치
- [ ] Docker & Docker Compose 설치
- [ ] Git 설치
- [ ] Node.js 18+ (로컬 개발 시)

### 2. 환경 설정
- [ ] `.env` 파일 생성 및 설정 (`.env.example` 참조)
- [ ] 외부 API 키 설정 (SendGrid, CoolSMS, AWS S3, NewsData.io)
- [ ] 데이터베이스 비밀번호 설정
- [ ] JWT 시크릿 키 설정

### 3. AWS 리소스 준비 (선택사항)
- [ ] EC2 인스턴스 생성 (t3.medium 이상 권장)
- [ ] 보안그룹 설정 (80, 443, 8080, 22 포트)
- [ ] S3 버킷 생성 및 CORS 설정
- [ ] IAM 역할 및 정책 설정

## 🛠️ 배포 단계

### 1단계: 프로젝트 준비
```bash
# 프로젝트 클론
git clone https://github.com/Iusto/byeolnight.git
cd byeolnight

# 환경변수 설정
cp .env.example .env
nano .env  # 실제 값으로 수정 (Windows: notepad .env)
```

### 2단계: 로컬 개발 환경 실행
```bash
# 데이터베이스 및 Redis 실행
docker-compose -f docker-compose.local.yml up -d

# Spring Boot 애플리케이션 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# 프론트엔드 실행 (새 터미널)
cd byeolnight-frontend
npm install
npm run dev
```

### 3단계: Docker 전체 배포
```bash
# 전체 서비스 빌드 및 실행
docker-compose up --build -d

# 서비스 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f
```

### 4단계: EC2 배포 (선택사항)
```bash
# EC2 접속
ssh -i your-key.pem ubuntu@your-ec2-ip

# 필수 소프트웨어 설치
sudo apt update
sudo apt install -y docker.io docker-compose-plugin openjdk-21-jdk
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ubuntu

# 재로그인 후 프로젝트 배포
git clone https://github.com/Iusto/byeolnight.git
cd byeolnight
cp .env.example .env
nano .env  # 실제 값으로 수정
docker-compose up --build -d
```

## 🔧 배포 스크립트

### Windows 배포 스크립트
```batch
# 전체 배포
./deploy.bat

# 로컬 개발 환경
./run-local.bat
```

### Linux/Mac 배포 명령어
```bash
# 전체 배포
docker-compose up --build -d

# 빠른 재배포 (코드 변경 시)
docker-compose down
docker-compose up --build -d

# 특정 서비스만 재시작
docker-compose restart backend
docker-compose restart frontend
```

### 헬스체크
```bash
# 서비스 상태 확인
docker-compose ps

# API 헬스체크
curl http://localhost:8080/actuator/health

# 프론트엔드 접속 확인
curl http://localhost
```

## 📊 모니터링

### 실시간 로그 확인
```bash
# 전체 로그
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f app          # Spring Boot 백엔드
docker-compose logs -f frontend     # React 프론트엔드
docker-compose logs -f mysql        # MySQL 데이터베이스
docker-compose logs -f redis        # Redis 캐시
```

### 서비스 접속 확인
```bash
# 웹사이트 접속
http://localhost              # 프론트엔드
http://localhost:8080         # 백엔드 API
http://localhost:8080/swagger-ui.html  # API 문서

# 데이터베이스 접속 (선택사항)
mysql -h localhost -P 3306 -u byeolnight -p
```

### 리소스 사용량 확인
```bash
# 컨테이너 리소스 사용량
docker stats

# 컨테이너 상태 확인
docker-compose ps

# 디스크 사용량 확인
docker system df
```

## 🔒 보안 설정

### 환경변수 보안
```bash
# .env 파일 권한 설정 (Linux/Mac)
chmod 600 .env

# 중요: .env 파일을 Git에 커밋하지 마세요!
echo ".env" >> .gitignore
```

### 필수 보안 설정
- **JWT_SECRET**: 복잡한 시크릿 키 설정 (최소 32자)
- **DB_PASSWORD**: 강력한 데이터베이스 비밀번호
- **REDIS_PASSWORD**: Redis 비밀번호 설정
- **API 키들**: 실제 서비스 키로 교체

### EC2 보안 그룹 설정
```bash
# 인바운드 규칙
- HTTP (80): 0.0.0.0/0
- HTTPS (443): 0.0.0.0/0  
- Custom TCP (8080): 0.0.0.0/0  # API 접근용
- SSH (22): Your IP only
```

### SSL 인증서 설정 (선택사항)
```bash
# Let's Encrypt 설치
sudo apt install -y certbot
sudo certbot certonly --standalone -d your-domain.com

# Nginx 설정에 SSL 적용
# (자세한 내용은 nginx.conf 참조)
```

## 🚨 트러블슈팅

### 자주 발생하는 문제들

#### 1. 포트 충돌 오류
```bash
# 포트 사용 중인 프로세스 확인
netstat -ano | findstr :8080  # Windows
lsof -i :8080                 # Linux/Mac

# 해결: 해당 프로세스 종료 후 재시작
```

#### 2. Docker 빌드 실패
```bash
# Docker 캐시 정리
docker system prune -f
docker-compose down -v
docker-compose up --build -d
```

#### 3. 데이터베이스 연결 실패
```bash
# MySQL 컨테이너 로그 확인
docker-compose logs mysql

# 환경변수 확인
cat .env | grep DB_

# 해결: .env 파일의 DB 설정 확인
```

#### 4. 프론트엔드 빌드 오류
```bash
# Node.js 버전 확인 (18+ 필요)
node --version

# 의존성 재설치
cd byeolnight-frontend
rm -rf node_modules package-lock.json
npm install
```

#### 5. 메모리 부족 (EC2)
```bash
# 스왑 파일 생성
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# 영구 적용
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

### 컨테이너 관리
```bash
# 특정 서비스 재시작
docker-compose restart app
docker-compose restart frontend

# 전체 재시작
docker-compose restart

# 완전 재배포
docker-compose down
docker-compose up --build -d
```

### 데이터베이스 관리
```bash
# 데이터베이스 백업
docker exec byeolnight-mysql mysqldump -u byeolnight -p byeolnight > backup.sql

# 데이터베이스 복원
docker exec -i byeolnight-mysql mysql -u byeolnight -p byeolnight < backup.sql

# 주의: 데이터베이스 초기화 (모든 데이터 삭제!)
docker-compose down -v
docker-compose up -d
```

## 📈 성능 최적화

### JVM 메모리 설정
```yaml
# docker-compose.yml에서 메모리 설정
services:
  app:
    environment:
      - JAVA_OPTS=-Xmx2g -Xms1g -XX:+UseG1GC
    deploy:
      resources:
        limits:
          memory: 3g
        reservations:
          memory: 1g
```

### 프론트엔드 최적화
```bash
# 프로덕션 빌드 최적화
cd byeolnight-frontend
npm run build

# 빌드 결과 확인
ls -la dist/
```

### 데이터베이스 최적화
```sql
-- MySQL 설정 최적화 (my.cnf)
[mysqld]
innodb_buffer_pool_size = 1G
innodb_log_file_size = 256M
max_connections = 200
```

### Nginx 프록시 설정 (선택사항)
```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    # 프론트엔드
    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
    
    # 백엔드 API
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
    
    # WebSocket
    location /ws {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }
}
```

## 🔄 업데이트 및 배포 절차

### 로컬 개발 업데이트
```bash
# 코드 변경 후
git pull origin main

# 백엔드 재시작
./gradlew bootRun --args='--spring.profiles.active=local'

# 프론트엔드 재시작 (자동 핫 리로드)
# npm run dev가 실행 중이면 자동으로 반영됨
```

### Docker 배포 업데이트
```bash
# 코드 변경 후
git pull origin main

# 전체 재배포
docker-compose down
docker-compose up --build -d

# 또는 특정 서비스만 업데이트
docker-compose up --build -d app
```

### EC2 프로덕션 업데이트
```bash
# EC2 서버에서
cd byeolnight
git pull origin main

# 무중단 배포 (Blue-Green)
docker-compose up --build -d --no-deps app

# 헬스체크
curl http://localhost:8080/actuator/health
```

## 📊 배포 후 검증 체크리스트

### 기본 기능 테스트
- [ ] 웹사이트 접속: `http://localhost`
- [ ] API 문서 접속: `http://localhost:8080/swagger-ui.html`
- [ ] 회원가입/로그인 기능
- [ ] 게시글 작성/조회 기능
- [ ] 실시간 채팅 기능
- [ ] 파일 업로드 기능
- [ ] 쪽지 시스템 기능
- [ ] 실시간 알림 기능

### 관리자 기능 테스트
- [ ] 관리자 로그인
- [ ] 사용자 관리 기능
- [ ] 게시글 관리 기능
- [ ] 시스템 로그 확인

### 성능 및 안정성 확인
- [ ] 메모리 사용량 확인: `docker stats`
- [ ] 로그 에러 확인: `docker-compose logs -f`
- [ ] 데이터베이스 연결 상태
- [ ] Redis 캐시 동작 확인

## 📞 지원 및 문의

### 문제 발생 시 진단 순서
1. **로그 확인**: `docker-compose logs -f`
2. **서비스 상태 확인**: `docker-compose ps`
3. **리소스 사용량 확인**: `docker stats`
4. **네트워크 연결 확인**: `curl http://localhost:8080/actuator/health`
5. **환경변수 확인**: `.env` 파일 설정 검토

### 연락처
- **개발자**: 김정규
- **이메일**: iusto@naver.com
- **GitHub**: [@Iusto](https://github.com/Iusto)
- **이슈 등록**: [GitHub Issues](https://github.com/Iusto/byeolnight/issues)

### 추가 리소스
- **메인 README**: [README.md](README.md)
- **API 문서**: http://localhost:8080/swagger-ui.html
- **프로젝트 위키**: GitHub Wiki (예정)

---

> **💡 팁**: 배포 전에 항상 로컬 환경에서 충분히 테스트하고, 중요한 데이터는 정기적으로 백업하세요!