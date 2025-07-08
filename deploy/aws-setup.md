# 🚀 AWS 배포 가이드

## 📋 권장 AWS 구성

### 최소 구성 (개발/테스트)
- **EC2**: t3.medium (2 vCPU, 4GB RAM) - 월 $30-40
- **스토리지**: 20GB gp3
- **보안그룹**: 80, 443, 8080, 22 포트 오픈

### 운영 환경 구성 (권장)
- **EC2**: t3.large (2 vCPU, 8GB RAM) - 월 $60-80
- **RDS MySQL**: db.t3.micro - 월 $15-20
- **ElastiCache Redis**: cache.t3.micro - 월 $15-20
- **S3**: 파일 저장소 - 사용량에 따라
- **총 비용**: 월 $90-120

## 🔧 EC2 초기 설정

```bash
# 1. 시스템 업데이트
sudo yum update -y

# 2. Docker 설치
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -a -G docker ec2-user

# 3. Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 4. Git 설치
sudo yum install -y git

# 5. 프로젝트 클론
git clone https://github.com/your-username/byeolnight.git
cd byeolnight
```

## 🌐 도메인 및 SSL 설정

### nginx 설정 (선택사항)
```bash
# nginx 설치
sudo yum install -y nginx

# SSL 인증서 (Let's Encrypt)
sudo yum install -y certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
```

## 📊 모니터링 설정

### CloudWatch 로그 설정
```bash
# CloudWatch 에이전트 설치
wget https://s3.amazonaws.com/amazoncloudwatch-agent/amazon_linux/amd64/latest/amazon-cloudwatch-agent.rpm
sudo rpm -U ./amazon-cloudwatch-agent.rpm
```

## 🔒 보안 설정

### 보안그룹 규칙
- **SSH (22)**: 내 IP만 허용
- **HTTP (80)**: 0.0.0.0/0
- **HTTPS (443)**: 0.0.0.0/0
- **App (8080)**: 0.0.0.0/0 (또는 ALB에서만)

### IAM 역할 설정
- S3 접근 권한
- CloudWatch 로그 권한
- RDS 접근 권한 (필요시)

## 🚀 배포 명령어

```bash
# 배포 스크립트 실행 권한 부여
chmod +x deploy/deploy.sh

# 배포 실행
./deploy/deploy.sh
```

## 📈 성능 최적화

### JVM 옵션 설정
```bash
# docker-compose.yml에 추가
environment:
  - JAVA_OPTS=-Xmx2g -Xms1g -XX:+UseG1GC
```

### 데이터베이스 최적화
- RDS 파라미터 그룹 설정
- 커넥션 풀 최적화
- 인덱스 최적화