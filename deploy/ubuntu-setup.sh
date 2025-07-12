#!/bin/bash

echo "🐧 Ubuntu 서버 초기 설정 시작..."

# 시스템 업데이트
echo "📦 시스템 업데이트 중..."
sudo apt update && sudo apt upgrade -y

# 필수 패키지 설치
echo "🔧 필수 패키지 설치 중..."
sudo apt install -y curl wget git htop unzip jq

# Java 21 설치
echo "☕ Java 21 설치 중..."
sudo apt install -y openjdk-21-jdk
java -version

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

# Docker 서비스 시작
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ubuntu

# 방화벽 설정
echo "🔥 방화벽 설정 중..."
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 8080/tcp
sudo ufw --force enable

# 스왑 파일 생성 (메모리 부족 방지)
echo "💾 스왑 파일 생성 중..."
if [ ! -f /swapfile ]; then
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
  echo "✅ 2GB 스왑 파일 생성 완료"
else
  echo "✅ 스왑 파일이 이미 존재합니다"
fi

# 시스템 정보 출력
echo ""
echo "🎉 Ubuntu 서버 설정 완료!"
echo "📊 시스템 정보:"
echo "- OS: $(lsb_release -d | cut -f2)"
echo "- Java: $(java -version 2>&1 | head -n1)"
echo "- Docker: $(docker --version)"
echo "- Docker Compose: $(docker compose version)"
echo "- 메모리: $(free -h | grep Mem | awk '{print $2}')"
echo "- 디스크: $(df -h / | tail -1 | awk '{print $4}') 사용 가능"
echo ""
echo "⚠️  재로그인 후 'docker' 명령어를 sudo 없이 사용할 수 있습니다."
echo "🔄 재로그인: exit 후 다시 SSH 접속"