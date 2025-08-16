# 08. 배포 가이드

> 로컬 개발부터 운영 배포까지의 전체 과정을 단계별로 설명합니다.

## 🚀 배포 환경 구성

### 환경별 특징

| 환경 | 목적 | 구성 | 접근 방법 |
|------|------|------|-----------|
| **Local** | 개발 및 디버깅 | Backend + DB만 Docker, Frontend는 Vite 개발서버 | `./run-local.bat` |
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

# 2. 환경변수 설정
cp .env.example .env
# .env 파일을 열어서 실제 값들로 수정
```

### 3. 환경변수 설정 (.env 파일)
```bash
# 데이터베이스 설정
DB_HOST=localhost
DB_PORT=3306
DB_NAME=byeolnight
DB_USERNAME=root
DB_PASSWORD=your_strong_password

# Redis 설정
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# JWT 설정
JWT_SECRET=your_very_long_and_secure_jwt_secret_key_here
JWT_ACCESS_TOKEN_EXPIRATION=1800000  # 30분
JWT_REFRESH_TOKEN_EXPIRATION=604800000  # 7일

# AWS S3 설정
AWS_ACCESS_KEY_ID=your_aws_access_key
AWS_SECRET_ACCESS_KEY=your_aws_secret_key
AWS_REGION=ap-northeast-2
AWS_S3_BUCKET=your-s3-bucket-name

# 이메일 설정 (SendGrid)
SENDGRID_API_KEY=your_sendgrid_api_key
SENDGRID_FROM_EMAIL=noreply@yourdomain.com

# Gmail SMTP (백업)
GMAIL_USERNAME=your_gmail@gmail.com
GMAIL_PASSWORD=your_app_password

# SMS 설정 (CoolSMS)
COOLSMS_API_KEY=your_coolsms_api_key
COOLSMS_API_SECRET=your_coolsms_secret
COOLSMS_FROM_NUMBER=your_phone_number

# 외부 API 설정
NEWSDATA_API_KEY=your_newsdata_api_key
GOOGLE_VISION_API_KEY=your_google_vision_api_key
OPENAI_API_KEY=your_openai_api_key
CLAUDE_API_KEY=your_claude_api_key
```

### 4. 로컬 실행 방법

#### 방법 1: 개발 모드 (권장)
```bash
# 백엔드 + DB만 Docker로 실행
docker-compose -f docker-compose.local.yml up -d

# 백엔드 Spring Boot 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# 프론트엔드 개발 서버 실행 (별도 터미널)
cd byeolnight-frontend
npm install
npm run dev
```

#### 방법 2: 전체 Docker 실행
```bash
# 전체 서비스 컨테이너로 실행
docker-compose up --build -d
```

### 5. 접속 URL
- **개발 모드**: 
  - Frontend: http://localhost:5173
  - Backend: http://localhost:8080
  - API 문서: http://localhost:8080/swagger-ui.html
- **Docker 모드**: http://localhost

## 🐳 Docker 배포

### Docker Compose 구성

```yaml
# docker-compose.yml (운영용)
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: ${DB_NAME}
    volumes:
      - mysql_data:/var/lib/mysql
    ports:
      - "3306:3306"

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
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
# 전체 서비스 빌드 및 실행
docker-compose up --build -d

# 로그 확인
docker-compose logs -f

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
cp .env.example .env
# .env 파일 수정 (운영 환경 값으로)
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

## 🔄 CI/CD 파이프라인 (GitHub Actions)

### .github/workflows/deploy.yml

```yaml
name: Deploy to AWS EC2

on:
  push:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v3
      
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        
    - name: Run tests
      run: ./gradlew test
      
    - name: Build application
      run: ./gradlew build
      
    - name: Build Docker image
      run: |
        docker build -t byeolnight-backend .
        docker build -t byeolnight-frontend ./byeolnight-frontend
        
    - name: Deploy to EC2
      uses: appleboy/ssh-action@v0.1.5
      with:
        host: ${{ secrets.EC2_HOST }}
        username: ec2-user
        key: ${{ secrets.EC2_SSH_KEY }}
        script: |
          cd /home/ec2-user/byeolnight
          git pull origin main
          docker-compose down
          docker-compose up --build -d
```

### GitHub Secrets 설정

```bash
# Repository Settings > Secrets and variables > Actions에서 설정
EC2_HOST=your-ec2-public-ip
EC2_SSH_KEY=your-private-key-content
DB_PASSWORD=your-production-db-password
JWT_SECRET=your-production-jwt-secret
# ... 기타 운영 환경 변수들
```

## 🔍 배포 후 확인사항

### 헬스체크 엔드포인트

```bash
# 백엔드 상태 확인
curl http://your-domain/api/health

# 데이터베이스 연결 확인
curl http://your-domain/api/health/db

# Redis 연결 확인
curl http://your-domain/api/health/redis
```

### 로그 모니터링

```bash
# 애플리케이션 로그 확인
docker-compose logs -f backend

# 에러 로그만 필터링
docker-compose logs backend | grep ERROR

# 실시간 로그 모니터링
tail -f /var/log/byeolnight/application.log
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

### 자주 발생하는 문제들

#### 1. 메모리 부족 오류
```bash
# 해결: EC2 인스턴스 타입 업그레이드 또는 스왑 메모리 추가
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

#### 2. 데이터베이스 연결 실패
```bash
# 해결: 보안 그룹 및 방화벽 설정 확인
sudo ufw allow 3306
# RDS 보안 그룹에서 EC2 보안 그룹 허용
```

#### 3. SSL 인증서 갱신 실패
```bash
# 해결: Certbot 자동 갱신 설정
sudo crontab -e
# 다음 라인 추가: 0 12 * * * /usr/bin/certbot renew --quiet
```

## 📊 배포 체크리스트

### 배포 전 확인사항
- [ ] 모든 테스트 통과 확인
- [ ] 환경변수 설정 완료
- [ ] 데이터베이스 마이그레이션 스크립트 준비
- [ ] SSL 인증서 유효성 확인
- [ ] 백업 계획 수립

### 배포 후 확인사항
- [ ] 헬스체크 엔드포인트 정상 응답
- [ ] 주요 기능 동작 확인 (로그인, 게시글 작성 등)
- [ ] 로그 에러 없음 확인
- [ ] 성능 지표 정상 범위 확인
- [ ] 모니터링 알람 설정 확인

---

👉 다음 문서: [09. 로드맵](./09_roadmap.md)