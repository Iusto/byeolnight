#!/bin/bash

# Config Server 암호화 스크립트
# 사용법: ./encrypt-config.sh "암호화할_값"

CONFIG_SERVER_URL="http://localhost:8888"
CONFIG_USER="config-admin"
CONFIG_PASS="config-secret-2024"

if [ -z "$1" ]; then
    echo "사용법: $0 \"암호화할_값\""
    exit 1
fi

VALUE_TO_ENCRYPT="$1"

echo "Config Server에서 값을 암호화하는 중..."
echo "원본 값: $VALUE_TO_ENCRYPT"

ENCRYPTED_VALUE=$(curl -s -u "$CONFIG_USER:$CONFIG_PASS" \
    -H "Content-Type: text/plain" \
    -d "$VALUE_TO_ENCRYPT" \
    "$CONFIG_SERVER_URL/encrypt")

if [ $? -eq 0 ] && [ -n "$ENCRYPTED_VALUE" ]; then
    echo "암호화된 값: {cipher}$ENCRYPTED_VALUE"
    echo ""
    echo "YAML 파일에 다음과 같이 사용하세요:"
    echo "property: '{cipher}$ENCRYPTED_VALUE'"
else
    echo "암호화 실패. Config Server가 실행 중인지 확인하세요."
    exit 1
fi