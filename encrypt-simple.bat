@echo off
if "%1"=="" (
    echo Usage: encrypt-simple.bat text-to-encrypt
    echo Example: encrypt-simple.bat mypassword123
    exit /b 1
)

echo Encrypting: %1
curl -X POST http://localhost:8888/encrypt -H "Content-Type: text/plain" -d "%1"