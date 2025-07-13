#!/bin/bash

echo "🐧 Ubuntu 서버 초기 설정 시작..."

# 시스템 업데이트
echo "📦 시스템 업데이트 중..."
sudo apt update && sudo apt upgrade -y

# 필수 패키지 설치
echo "🔧 필수 패키지 설치 중..."
sudo apt install -y curl wget git htop unzip jq openjdk-21-jdk

# Docker 설치
echo "🐳 Docker 설치 중..."
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker ubuntu

# 방화벽 설정
echo "🔥 방화벽 설정 중..."
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 8080/tcp
sudo ufw --force enable

# 스왑 파일 생성
echo "💾 스왑 파일 생성 중..."
if [ ! -f /swapfile ]; then
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
fi

echo "🎉 Ubuntu 서버 설정 완료!"
echo "⚠️ 재로그인 후 Docker 명령어를 sudo 없이 사용할 수 있습니다."