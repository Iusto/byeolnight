@echo off
REM Config Server 암호화 스크립트 (Windows용)
REM 사용법: encrypt-config.bat "암호화할_값"

set CONFIG_SERVER_URL=http://localhost:8888
set CONFIG_USER=config-admin
set CONFIG_PASS=config-secret-2024

if "%~1"=="" (
    echo 사용법: %0 "암호화할_값"
    exit /b 1
)

set VALUE_TO_ENCRYPT=%~1

echo Config Server에서 값을 암호화하는 중...
echo 원본 값: %VALUE_TO_ENCRYPT%

curl -s -u "%CONFIG_USER%:%CONFIG_PASS%" -H "Content-Type: text/plain" -d "%VALUE_TO_ENCRYPT%" "%CONFIG_SERVER_URL%/encrypt" > temp_encrypted.txt

if %errorlevel% equ 0 (
    set /p ENCRYPTED_VALUE=<temp_encrypted.txt
    echo 암호화된 값: {cipher}!ENCRYPTED_VALUE!
    echo.
    echo YAML 파일에 다음과 같이 사용하세요:
    echo property: '{cipher}!ENCRYPTED_VALUE!'
    del temp_encrypted.txt
) else (
    echo 암호화 실패. Config Server가 실행 중인지 확인하세요.
    del temp_encrypted.txt 2>nul
    exit /b 1
)