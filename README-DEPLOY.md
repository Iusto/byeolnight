# 🚀 배포 가이드

## 📋 배포 전 체크리스트

### 1. 환경 설정
- [ ] `.env` 파일 생성 및 설정
- [ ] AWS 계정 및 키 설정
- [ ] 도메인 설정 (선택사항)
- [ ] SSL 인증서 설정 (선택사항)

### 2. AWS 리소스 준비
- [ ] EC2 인스턴스 생성 (t3.medium 이상 권장)
- [ ] 보안그룹 설정 (80, 443, 8080, 22 포트)
- [ ] S3 버킷 생성
- [ ] IAM 역할 설정

## 🛠️ 배포 단계

### 1단계: Ubuntu 서버 초기 설정
```bash
# Ubuntu EC2 인스턴스에 접속 후
chmod +x deploy/ubuntu-setup.sh
./deploy/ubuntu-setup.sh

# 재로그인 필요 (Docker 그룹 적용)
exit
ssh -i your-key.pem ubuntu@your-ec2-ip

# Docker 작동 확인
docker --version
docker compose version
```

### 2단계: 프로젝트 배포
```bash
# 프로젝트 클론
git clone https://github.com/your-username/byeolnight.git
cd byeolnight

# 환경변수 설정
cp deploy/production.env .env
nano .env  # 실제 값으로 수정

# 배포 실행
chmod +x deploy/deploy.sh
./deploy/deploy.sh
```

### 3단계: 서비스 확인
```bash
# 헬스체크 실행
chmod +x deploy/health-check.sh
./deploy/health-check.sh

# 로그 확인
docker-compose logs -f
```

## 🔧 배포 스크립트

### 전체 배포
```bash
./deploy/deploy.sh
```

### 빠른 재배포 (코드 변경 시)
```bash
./deploy/quick-deploy.sh
```

### 헬스체크
```bash
./deploy/health-check.sh
```

## 📊 모니터링

### 실시간 로그 확인
```bash
# 전체 로그
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mysql
docker-compose logs -f redis
```

### 리소스 사용량 확인
```bash
# 컨테이너 리소스 사용량
docker stats

# 시스템 리소스 확인
htop
df -h
free -h
```

## 🔒 보안 설정

### 방화벽 설정
```bash
# 필요한 포트만 열기 (Ubuntu UFW)
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 8080/tcp
sudo ufw --force enable
```

### SSL 인증서 설정 (Let's Encrypt)
```bash
sudo apt install -y certbot
sudo certbot certonly --standalone -d your-domain.com
```

## 🚨 트러블슈팅

### 메모리 부족 시
```bash
# 스왑 파일 생성
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

### 컨테이너 재시작
```bash
# 특정 서비스 재시작
docker-compose restart backend

# 전체 재시작
docker-compose restart
```

### 데이터베이스 초기화
```bash
# 주의: 모든 데이터가 삭제됩니다!
docker-compose down -v
docker-compose up -d
```

## 📈 성능 최적화

### JVM 메모리 설정
```yaml
# docker-compose.yml
services:
  backend:
    environment:
      - JAVA_OPTS=-Xmx2g -Xms1g -XX:+UseG1GC
```

### Nginx 프록시 설정 (선택사항)
```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 🔄 업데이트 절차

1. 코드 변경 후 Git 푸시
2. EC2에서 배포 스크립트 실행
3. 헬스체크로 서비스 상태 확인
4. 문제 발생 시 롤백

```bash
# 업데이트 배포
git pull origin main
./deploy/deploy.sh

# 헬스체크
./deploy/health-check.sh
```

## 📞 지원

문제 발생 시:
1. 로그 확인: `docker-compose logs -f`
2. 헬스체크 실행: `./deploy/health-check.sh`
3. 이슈 등록: GitHub Issues