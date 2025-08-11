# 08. 배포 가이드

> Spring Cloud Config Server 기반 중앙화된 설정 관리를 통한 로컬 개발부터 운영 배포까지의 전체 과정을 설명합니다.

## 🚀 배포 환경 구성

### 환경별 특징

| 환경 | 목적 | 구성 | 접근 방법 |
|------|------|------|-----------|
| **Local** | 개발 및 디버깅 | Config Server + Backend + DB, Frontend는 Vite 개발서버 | Config Server 먼저 시작 후 애플리케이션 실행 |
| **Docker** | 통합 테스트 및 배포 | 전체 서비스 컨테이너화 (Config Server 포함) | `docker-compose up` |
| **Production** | 실제 운영 | AWS EC2 + RDS + S3 + Config Server | GitHub Actions 자동 배포 |

## 🛠️ 로컬 개발 환경 설정

### 1. 필수 요구사항
```bash
# 필수 소프트웨어
- Java 21 이상
- Node.js 18 이상
- Docker & Docker Compose
- Git
```

### 2. Config Server 기반 프로젝트 설정
```bash
# 1. 저장소 클론
git clone https://github.com/your-username/byeolnight.git
cd byeolnight

# 2. Config Server 설정 파일 준비
# config-repo/configs/byeolnight-local.yml 파일에 실제 설정 값 입력
# (암호화된 값은 {cipher}로 시작)

# 3. Config Server 먼저 시작 (별도 터미널)
cd config-server
gradlew bootRun

# 4. 메인 애플리케이션 시작 (별도 터미널)
cd ..
gradlew bootRun --args='--spring.profiles.active=local'
```

### 3. Config Server 설정 파일 구성

#### config-repo/configs/byeolnight-local.yml
```yaml
# 데이터베이스 설정
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/byeolnight
    username: root
    password: '{cipher}AQA...암호화된_패스워드'
  
  # Redis 설정
  data:
    redis:
      host: localhost
      port: 6379
      password: '{cipher}AQB...암호화된_패스워드'

# JWT 설정
jwt:
  secret: '{cipher}AQC...암호화된_시크릿키'
  access-token-expiration: 1800000  # 30분
  refresh-token-expiration: 604800000  # 7일

# AWS S3 설정
aws:
  access-key-id: '{cipher}AQD...암호화된_액세스키'
  secret-access-key: '{cipher}AQE...암호화된_시크릿키'
  region: ap-northeast-2
  s3:
    bucket: your-s3-bucket-name

# 이메일 설정
gmail:
  username: your_gmail@gmail.com
  password: '{cipher}AQG...암호화된_앱패스워드'

# 외부 API 설정
newsdata:
  api-key: '{cipher}AQJ...암호화된_API키'

google:
  vision:
    api-key: '{cipher}AQK...암호화된_API키'

openai:
  api-key: '{cipher}AQL...암호화된_API키'
```

#### 설정 값 암호화 방법
```bash
# Config Server의 암호화 엔드포인트 사용
curl -u config-admin:config-secret-2024 \
  -X POST http://localhost:8888/encrypt \
  -d "your_secret_value"

# 응답으로 받은 암호화된 값을 {cipher}접두사와 함께 설정 파일에 입력
```

### 4. 로컬 실행 방법

#### 방법 1: Config Server 기반 개발 모드 (권장)
```bash
# 1. Config Server 시작 (첫 번째 터미널)
cd config-server
gradlew bootRun

# 2. 데이터베이스 서비스만 Docker로 실행 (두 번째 터미널)
docker-compose -f docker-compose.local.yml up -d mysql redis

# 3. 메인 애플리케이션 시작 (세 번째 터미널)
cd ..
gradlew bootRun --args='--spring.profiles.active=local'

# 4. 프론트엔드 개발 서버 실행 (네 번째 터미널)
cd byeolnight-frontend
npm install
npm run dev
```

#### 방법 2: 전체 Docker 실행 (Config Server 포함)
```bash
# config-repo 설정 완료 후 전체 서비스 컨테이너로 실행
docker-compose up --build -d
```

### 5. 접속 URL
- **Config Server**: http://localhost:8888 (인증: config-admin/config-secret-2024)
- **개발 모드**: 
  - Frontend: http://localhost:5173
  - Backend: http://localhost:8080
  - API 문서: http://localhost:8080/swagger-ui.html
- **Docker 모드**: http://localhost

### 6. Config Server 설정 확인
```bash
# 현재 적용된 설정 조회
curl -u config-admin:config-secret-2024 \
  http://localhost:8888/byeolnight/local

# 특정 프로퍼티 값 확인
curl -u config-admin:config-secret-2024 \
  http://localhost:8888/byeolnight/local/master
```

## 🐳 Docker 배포

### Docker Compose 구성 (Config Server 포함)

```yaml
# docker-compose.yml (운영용)
version: '3.8'
services:
  config-server:
    build: ./config-server
    ports:
      - "8888:8888"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    volumes:
      - ./config-repo:/app/config-repo
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8888/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

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
      - SPRING_CLOUD_CONFIG_URI=http://config-server:8888
    depends_on:
      config-server:
        condition: service_healthy
      mysql:
        condition: service_started
      redis:
        condition: service_started
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
# Config Server 설정 파일 준비 확인
ls -la config-repo/configs/

# 전체 서비스 빌드 및 실행 (Config Server 먼저 시작)
docker-compose up --build -d

# Config Server 상태 확인
docker-compose logs config-server
curl -u config-admin:config-secret-2024 http://localhost:8888/actuator/health

# 전체 로그 확인
docker-compose logs -f

# 특정 서비스 로그 확인
docker-compose logs -f backend

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

### EC2 인스턴스 설정 (Config Server 기반)

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

# 5. Config Server 설정 파일 준비
# config-repo/configs/byeolnight-docker.yml 파일에 운영 환경 설정 입력
# (모든 민감한 정보는 암호화하여 저장)

# 6. Config Server 먼저 시작하여 상태 확인
docker-compose up -d config-server
docker-compose logs config-server

# 7. 전체 서비스 시작
docker-compose up --build -d
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

### 자동화된 워크플로우 (5개)

1. **CI 테스트** (`.github/workflows/ci.yml`)
   - 코드 푸시 시 자동 테스트 실행
   - 백엔드 + 프론트엔드 빌드 검증

2. **코드 품질 검사** (`.github/workflows/code-quality.yml`)
   - CodeQL 보안 스캔
   - 의존성 취약점 검사

3. **자동 배포** (`.github/workflows/deploy.yml`)
   - `master` 브랜치 푸시 시 운영 서버 자동 배포
   - Docker 이미지 빌드 및 GHCR 푸시
   - SSH를 통한 운영 서버 배포

4. **PR 검증** (`.github/workflows/pr-check.yml`)
   - Pull Request 제목 컨벤션 체크
   - PR 크기 검증
   - 자동 리뷰어 할당

5. **성능 테스트** (`.github/workflows/performance.yml`)
   - 주기적 부하 테스트
   - Lighthouse 성능 측정

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
      
    - name: Config Server 빌드
      run: |
        cd config-server
        chmod +x ./gradlew
        ./gradlew build -x test
        
    - name: GitHub Container Registry 로그인
      uses: docker/login-action@v3
      with:
        registry: ghcr.io
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}
        
    - name: 백엔드/프론트엔드/Config Server 이미지 빌드 및 푸시
      # 3개 서비스 이미지 동시 빌드
      
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
          docker pull ghcr.io/iusto/byeolnight-config-server:latest
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

# Config Server 관련 설정
CONFIG_SERVER_USERNAME=config-admin
CONFIG_SERVER_PASSWORD=config-secret-2024
CONFIG_ENCRYPT_KEY=your-config-encryption-key

# 주의: 모든 민감한 설정은 Config Server의 config-repo에서 암호화하여 관리
# GitHub Secrets는 배포 관련 정보만 저장
```

### 배포 프로세스 (Config Server 포함)

1. **코드 푸시** → `master` 브랜치
2. **자동 빌드** → Java 21 + Gradle 빌드 (Config Server + Main App)
3. **Docker 이미지** → 3개 서비스 이미지 생성 (Config Server, Backend, Frontend)
4. **GHCR 푸시** → GitHub Container Registry에 저장
5. **Config 검증** → Config Server 설정 파일 유효성 검사
6. **서버 배포** → SSH로 운영 서버 접속하여 배포
7. **서비스 재시작** → Config Server 먼저 시작 후 나머지 서비스 순차 시작

## 🔍 배포 후 확인사항

### Config Server 상태 확인

```bash
# Config Server 헬스체크
curl -u config-admin:config-secret-2024 http://your-domain:8888/actuator/health

# 설정 조회 테스트
curl -u config-admin:config-secret-2024 http://your-domain:8888/byeolnight/docker

# 암호화/복호화 테스트
curl -u config-admin:config-secret-2024 -X POST http://your-domain:8888/encrypt -d "test"
```

### 애플리케이션 헬스체크 엔드포인트

```bash
# 백엔드 상태 확인
curl http://your-domain/api/health

# Config Server 연결 확인
curl http://your-domain/actuator/configprops

# 데이터베이스 연결 확인
curl http://your-domain/api/health/db

# Redis 연결 확인
curl http://your-domain/api/health/redis
```

### 로그 모니터링

```bash
# Config Server 로그 확인
docker-compose logs -f config-server

# 애플리케이션 로그 확인
docker-compose logs -f backend

# Config Server 설정 로딩 로그 확인
docker-compose logs config-server | grep "Located property source"

# 에러 로그만 필터링
docker-compose logs backend | grep ERROR

# 전체 서비스 로그 실시간 모니터링
docker-compose logs -f
```

### 성능 모니터링

```bash
# 시스템 리소스 확인
docker stats

# 데이터베이스 성능 확인
docker exec -it mysql_container mysql -u root -p -e "SHOW PROCESSLIST;"

# Redis 메모리 사용량 확인
docker exec -it redis_container redis-cli INFO memory
```

## 🚨 트러블슈팅

### Config Server 관련 문제

#### 1. Config Server 연결 실패
```bash
# Config Server 상태 확인
docker-compose logs config-server
curl -u config-admin:config-secret-2024 http://localhost:8888/actuator/health

# 애플리케이션에서 Config Server 연결 확인
curl http://localhost:8080/actuator/configprops | grep "spring.cloud.config"
```

#### 2. 설정 파일 로딩 실패
```bash
# config-repo 디렉토리 권한 확인
ls -la config-repo/configs/

# 설정 파일 문법 검증
yaml-lint config-repo/configs/byeolnight-docker.yml

# Config Server 로그에서 설정 로딩 확인
docker-compose logs config-server | grep "Located property source"
```

#### 3. 암호화/복호화 문제
```bash
# 암호화 키 설정 확인
docker-compose exec config-server env | grep ENCRYPT_KEY

# 수동 암호화 테스트
curl -u config-admin:config-secret-2024 -X POST http://localhost:8888/encrypt -d "test_value"

# 복호화 테스트
curl -u config-admin:config-secret-2024 -X POST http://localhost:8888/decrypt -d "AQA..."
```

### 일반적인 배포 문제

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
# Config Server 먼저 시작 후 다른 서비스 시작
docker-compose up -d config-server
# Config Server 준비 완료 확인 후
docker-compose up -d
```

### 성능 최적화 팁

#### Config Server 최적화
```bash
# Config Server JVM 옵션 설정
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# Config Server 캐시 설정 확인
curl -u config-admin:config-secret-2024 http://localhost:8888/actuator/env | grep cache
```

#### 애플리케이션 최적화
```bash
# Config 새로고침 없이 설정 변경 적용
curl -X POST http://localhost:8080/actuator/refresh

# Config Server 연결 풀 설정 확인
curl http://localhost:8080/actuator/metrics/http.client.requests
```

---

## 📝 배포 체크리스트

### 배포 전 확인사항
- [ ] Config Server 설정 파일 준비 완료
- [ ] 민감한 정보 암호화 완료
- [ ] 데이터베이스 마이그레이션 스크립트 준비
- [ ] 외부 API 키 유효성 확인
- [ ] SSL 인증서 유효기간 확인

### 배포 후 확인사항
- [ ] Config Server 정상 동작 확인
- [ ] 애플리케이션 헬스체크 통과
- [ ] 데이터베이스 연결 정상
- [ ] Redis 연결 정상
- [ ] 외부 API 연동 정상
- [ ] 로그 모니터링 설정 완료

이 가이드를 통해 Spring Cloud Config Server 기반의 안전하고 확장 가능한 배포 환경을 구축할 수 있습니다.