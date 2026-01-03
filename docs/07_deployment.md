# 08. 배포 가이드

> 로컬 개발부터 운영 배포까지 단계별로 쉽게 따라할 수 있는 가이드입니다.

## 🚀 배포 환경 구성

### 환경별 특징

| 환경 | 목적 | 구성 | 접근 방법 |
|------|------|------|-----------|
| **Local** | 개발 및 디버깅 | 백엔드 + DB, 프론트엔드는 개발서버 | 간단한 설정 파일로 실행 |
| **Docker** | 통합 테스트 및 배포 | 전체 서비스 컨테이너화 | `docker-compose up` |
| **Production** | 실제 운영 | AWS EC2 + RDS + S3 | GitHub Actions 자동 배포 |

## 🛠️ 로컬 개발 환경 설정

### 1. 필수 요구사항
```bash
# 필수 소프트웨어
- Java 21 이상
- Node.js 18 이상
- Docker & Docker Compose
- Git
```

### 2. 프로젝트 설정
```bash
# 1. 저장소 클론
git clone https://github.com/your-username/byeolnight.git
cd byeolnight

# 2. 환경 설정 파일 복사
cp application-local.yml.example application-local.yml
# application-local.yml 파일을 열어서 실제 값으로 수정

# 3. 백엔드 실행
gradlew bootRun --args='--spring.profiles.active=local'

# 4. 프론트엔드 실행 (새 터미널)
cd byeolnight-frontend
npm install
npm run dev
```

### 3. 환경 설정 파일 구성

#### application-local.yml (로컬 개발용)
```yaml
# 데이터베이스 설정
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/byeolnight
    username: root
    password: your_password
  
  # Redis 설정
  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password

# JWT 설정
jwt:
  secret: your-jwt-secret-key-at-least-32-characters
  access-token-expiration: 1800000  # 30분
  refresh-token-expiration: 604800000  # 7일

# AWS S3 설정 (선택사항)
aws:
  access-key-id: your-access-key
  secret-access-key: your-secret-key
  region: ap-northeast-2
  s3:
    bucket: your-s3-bucket-name

# 이메일 설정 (선택사항)
gmail:
  username: your_gmail@gmail.com
  password: your_app_password

# 외부 API 설정 (선택사항)
newsdata:
  api-key: your-newsdata-api-key

google:
  vision:
    api-key: your-google-vision-api-key

openai:
  api-key: your-openai-api-key
```

> **참고**: 실제 개발 시에는 필요한 설정만 입력하면 됩니다. 모든 외부 API가 필수는 아닙니다.

### 4. 로컬 실행 방법

#### 방법 1: 개발 모드 (권장)
```bash
# 1. 데이터베이스 서비스만 Docker로 실행
docker-compose -f docker-compose.local.yml up -d mysql redis

# 2. 백엔드 실행
gradlew bootRun --args='--spring.profiles.active=local'

# 3. 프론트엔드 실행 (새 터미널)
cd byeolnight-frontend
npm install
npm run dev
```

#### 방법 2: 전체 Docker 실행
```bash
# 전체 서비스를 컨테이너로 실행
docker-compose up --build -d
```

### 5. 접속 URL
- **개발 모드**: 
  - 프론트엔드: http://localhost:5173
  - 백엔드: http://localhost:8080
  - API 문서: http://localhost:8080/swagger-ui.html
- **Docker 모드**: http://localhost

### 6. 개발 환경 확인
```bash
# 백엔드 상태 확인
curl http://localhost:8080/actuator/health

# 데이터베이스 연결 확인
curl http://localhost:8080/actuator/health/db
```

## 🐳 Docker 배포

### Docker Compose 구성

```yaml
# docker-compose.yml (운영용)
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: your_strong_password
      MYSQL_DATABASE: byeolnight
    volumes:
      - mysql_data:/var/lib/mysql
    ports:
      - "3306:3306"

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass your_redis_password
    ports:
      - "6379:6379"

  backend:
    build: .
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      - mysql
      - redis
    ports:
      - "8080:8080"

  frontend:
    build: ./byeolnight-frontend
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  mysql_data:
```

### 배포 명령어
```bash
# 환경 설정 파일 준비 확인
ls -la application-docker.yml

# 전체 서비스 빌드 및 실행
docker-compose up --build -d

# 전체 로그 확인
docker-compose logs -f

# 특정 서비스 로그 확인
docker-compose logs -f backend

# 서비스 상태 확인
docker-compose ps

# 서비스 중지
docker-compose down

# 볼륨까지 완전 삭제
docker-compose down -v
```

## ☁️ AWS 운영 환경 배포

### 인프라 구성

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   CloudFront    │    │      ALB        │    │      EC2        │
│   (CDN/SSL)     │◄──►│  (Load Balancer)│◄──►│   (App Server)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                        │
                              ┌─────────────────────────┼─────────────────────────┐
                              ▼                         ▼                         ▼
                    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
                    │      RDS        │    │   ElastiCache   │    │       S3        │
                    │    (MySQL)      │    │    (Redis)      │    │  (File Storage) │
                    └─────────────────┘    └─────────────────┘    └─────────────────┘
```

### EC2 인스턴스 설정

```bash
# 1. EC2 인스턴스 접속
ssh -i your-key.pem ec2-user@your-ec2-ip

# 2. Docker 설치
sudo yum update -y
sudo yum install -y docker
sudo service docker start
sudo usermod -a -G docker ec2-user

# 3. Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 4. 프로젝트 배포
git clone https://github.com/your-username/byeolnight.git
cd byeolnight

# 5. 운영 환경 설정 파일 준비
cp application-docker.yml.example application-docker.yml
# application-docker.yml 파일을 열어서 운영 환경 값으로 수정

# 6. 전체 서비스 시작
docker-compose up --build -d

# 7. 서비스 상태 확인
docker-compose ps
docker-compose logs -f
```

### SSL 인증서 설정 (Let's Encrypt)

```bash
# Certbot 설치
sudo yum install -y certbot

# SSL 인증서 발급
sudo certbot certonly --standalone -d yourdomain.com

# Nginx 설정에 SSL 적용
# nginx/nginx.conf 파일 수정 후 재시작
docker-compose restart frontend
```

## 🔄 GitHub Actions 자동 배포

### 현재 CI/CD 파이프라인 구조

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Code Push     │    │  GitHub Actions │    │   Production    │
│   (master)      │───►│   Workflows     │───►│     Server      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ Container       │
                    │ Registry        │
                    │ (GHCR)          │
                    └─────────────────┘
```

### 자동화된 워크플로우 (6개)

1. **CI 테스트** (`.github/workflows/ci.yml`)
   - 코드 푸시 시 자동 테스트 실행
   - 백엔드 + 프론트엔드 빌드 검증

2. **코드 품질 검사** (`.github/workflows/code-quality.yml`)
   - CodeQL 보안 스캔
   - 의존성 취약점 검사

3. **배포 상태 기록** (`.github/workflows/deploy.yml`)
   - 수동 배포 완료 상태 기록
   - 실제 배포는 서버에서 `deploy.sh` 스크립트로 수행

4. **PR 검증** (`.github/workflows/pr-check.yml`)
   - Pull Request 제목 컨벤션 체크
   - PR 크기 검증
   - 자동 리뷰어 할당

5. **성능 테스트** (`.github/workflows/performance.yml`)
   - 주기적 부하 테스트
   - Lighthouse 성능 측정

6. **보안 강화** (`.github/workflows/security-enhanced.yml`)
   - 추가 보안 검사 및 모니터링

### 실제 배포 워크플로우

```yaml
# .github/workflows/deploy.yml (실제 파일 기준)
name: 배포

on:
  push:
    branches: [ master ]
  workflow_dispatch:  # 수동 실행 가능

jobs:
  build-and-push:
    name: Docker 이미지 빌드 및 푸시
    runs-on: ubuntu-latest
    steps:
    - name: 코드 체크아웃
      uses: actions/checkout@v4
      
    - name: JDK 21 설정
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        
    - name: 백엔드 빌드
      run: ./gradlew build -x test
        
    - name: GitHub Container Registry 로그인
      uses: docker/login-action@v3
      with:
        registry: ghcr.io
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}
        
    - name: 백엔드/프론트엔드 이미지 빌드 및 푸시
      # 2개 서비스 이미지 빌드
      
  deploy:
    name: 운영 서버 배포
    needs: build-and-push
    runs-on: ubuntu-latest
    steps:
    - name: 운영 서버 배포
      uses: appleboy/ssh-action@v1.0.3
      with:
        host: ${{ secrets.DEPLOY_HOST }}
        username: ${{ secrets.DEPLOY_USER }}
        key: ${{ secrets.DEPLOY_SSH_KEY }}
        script: |
          cd /opt/byeolnight
          docker pull ghcr.io/iusto/byeolnight-backend:latest
          docker pull ghcr.io/iusto/byeolnight-frontend:latest
          docker-compose down
          docker-compose up -d
```

### GitHub Secrets 설정

```bash
# Repository Settings > Secrets and variables > Actions에서 설정
DEPLOY_HOST=your-ec2-public-ip
DEPLOY_USER=ubuntu
DEPLOY_SSH_KEY=your-private-key-content

# 데이터베이스 설정
DB_PASSWORD=your-database-password
REDIS_PASSWORD=your-redis-password

# JWT 설정
JWT_SECRET=your-jwt-secret-key

# AWS 설정 (선택사항)
AWS_ACCESS_KEY_ID=your-aws-access-key
AWS_SECRET_ACCESS_KEY=your-aws-secret-key
```

### 실제 배포 프로세스

1. **서버 접속** → EC2 인스턴스에 SSH 접속
2. **코드 업데이트** → `git pull origin master`로 최신 코드 가져오기
3. **애플리케이션 빌드** → `./gradlew clean bootJar`로 빌드
4. **전체 서비스 배포** → `docker-compose up -d`로 전체 서비스 시작
5. **배포 완료 확인** → 로그 모니터링 및 상태 체크

## 🔍 배포 후 확인사항

### 애플리케이션 상태 확인

```bash
# 백엔드 상태 확인
curl http://your-domain/actuator/health

# 데이터베이스 연결 확인
curl http://your-domain/actuator/health/db

# Redis 연결 확인
curl http://your-domain/actuator/health/redis

# API 동작 확인
curl http://your-domain/api/posts
```

### 로그 모니터링

```bash
# 애플리케이션 로그 확인
docker-compose logs -f backend

# 에러 로그만 필터링
docker-compose logs backend | grep ERROR

# 전체 서비스 로그 실시간 모니터링
docker-compose logs -f

# 데이터베이스 로그 확인
docker-compose logs -f mysql
```

### 성능 모니터링

```bash
# 시스템 리소스 확인
docker stats

# 데이터베이스 성능 확인
docker exec -it byeolnight_mysql_1 mysql -u root -p -e "SHOW PROCESSLIST;"

# Redis 메모리 사용량 확인
docker exec -it byeolnight_redis_1 redis-cli INFO memory
```

## 🚨 트러블슈팅

### 일반적인 문제

#### 1. 애플리케이션 시작 실패
```bash
# 애플리케이션 로그 확인
docker-compose logs backend

# 데이터베이스 연결 확인
docker-compose exec backend curl http://localhost:8080/actuator/health/db
```

#### 2. 데이터베이스 연결 문제
```bash
# MySQL 서비스 상태 확인
docker-compose ps mysql

# MySQL 로그 확인
docker-compose logs mysql

# 연결 테스트
docker-compose exec mysql mysql -u root -p -e "SELECT 1;"
```

#### 3. Redis 연결 문제
```bash
# Redis 서비스 상태 확인
docker-compose ps redis

# Redis 연결 테스트
docker-compose exec redis redis-cli ping
```

#### 4. 메모리 부족 오류
```bash
# 해결: EC2 인스턴스 타입 업그레이드 또는 스왑 메모리 추가
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

#### 5. 데이터베이스 연결 실패
```bash
# 해결: 보안 그룹 및 방화벽 설정 확인
sudo ufw allow 3306
# RDS 보안 그룹에서 EC2 보안 그룹 허용
```

#### 6. 서비스 시작 순서 문제
```bash
# 데이터베이스 먼저 시작 후 애플리케이션 시작
docker-compose up -d mysql redis
# 데이터베이스 준비 완료 후
docker-compose up -d backend frontend
```



## 📝 배포 체크리스트

### 배포 전 확인사항
- [ ] 환경 설정 파일 준비 완료
- [ ] 데이터베이스 마이그레이션 스크립트 준비
- [ ] 외부 API 키 유효성 확인
- [ ] SSL 인증서 유효기간 확인
- [ ] Docker 이미지 빌드 테스트

### 배포 후 확인사항
- [ ] 애플리케이션 상태 확인
- [ ] 데이터베이스 연결 정상
- [ ] Redis 연결 정상
- [ ] API 동작 테스트
- [ ] 웹사이트 접속 테스트
- [ ] 로그 모니터링 설정 완료

이 가이드를 통해 안전하고 확장 가능한 배포 환경을 구축할 수 있습니다.