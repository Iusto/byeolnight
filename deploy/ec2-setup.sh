#!/bin/bash

echo "🚀 EC2 인스턴스 초기 설정 시작..."

# 시스템 업데이트
echo "📦 시스템 업데이트 중..."
sudo yum update -y

# Docker 설치
echo "🐳 Docker 설치 중..."
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -a -G docker ec2-user

# Docker Compose 설치
echo "🔧 Docker Compose 설치 중..."
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Git 설치
echo "📚 Git 설치 중..."
sudo yum install -y git

# 방화벽 설정 (Amazon Linux 2023)
echo "🔥 방화벽 설정 중..."
sudo systemctl start firewalld
sudo systemctl enable firewalld
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --permanent --add-port=443/tcp
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload

# 스왑 파일 생성 (메모리 부족 방지)
echo "💾 스왑 파일 생성 중..."
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

echo "✅ EC2 초기 설정 완료!"
echo "재로그인 후 Docker 명령어를 사용할 수 있습니다."