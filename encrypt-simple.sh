#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: ./encrypt-simple.sh \"text-to-encrypt\""
  exit 1
fi

echo "Encrypting: $1"
curl -X POST http://localhost:8888/encrypt \
  -H "Content-Type: text/plain" \
  -d "$1"
echo ""
