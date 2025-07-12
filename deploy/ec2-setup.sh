#!/bin/bash

echo "🚀 EC2 인스턴스 초기 설정 시작..."

# 시스템 업데이트
echo "📦 시스템 업데이트 중..."
sudo apt update && sudo apt upgrade -y

# Docker 설치
echo "🐳 Docker 설치 중..."
sudo apt install -y ca-certificates curl gnupg lsb-release

sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
  sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ubuntu

# Docker Compose 확인
echo "🔧 Docker Compose 버전 확인 중..."
docker compose version

# Git 설치
echo "📚 Git 설치 중..."
sudo apt install -y git

# 방화벽 설정 (Ubuntu: ufw)
echo "🔥 방화벽 설정 중..."
sudo apt install -y ufw
sudo ufw allow OpenSSH
sudo ufw allow 80
sudo ufw allow 443
sudo ufw allow 8080
sudo ufw --force enable

# 스왑 파일 생성
echo "💾 스왑 파일 생성 중..."
if [ ! -f /swapfile ]; then
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
else
  echo "✅ 스왑 파일이 이미 존재합니다. 생략합니다."
fi

echo "✅ EC2 초기 설정 완료!"
echo "재로그인 후 'docker' 명령어를 sudo 없이 사용할 수 있습니다."
