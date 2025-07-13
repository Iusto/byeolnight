# 🚀 배포 스크립트 가이드

## 📁 파일 구성

### 핵심 스크립트
- `deploy.sh` - 메인 배포 스크립트 (Linux/Mac)
- `health-check.sh` - 서비스 상태 확인
- `ubuntu-setup.sh` - Ubuntu 서버 초기 설정

### 로컬 개발용
- `run-local.bat` - Windows 로컬 개발 환경 시작
- `rebuild-docker.bat` - Windows Docker 재빌드
- `docker-compose.local.yml` - 로컬 개발용 Docker Compose

## 🛠️ 사용법

### 1. Ubuntu 서버 초기 설정
```bash
chmod +x ubuntu-setup.sh
./ubuntu-setup.sh
# 재로그인 필요
```

### 2. 프로젝트 배포
```bash
chmod +x deploy.sh
./deploy.sh                # 기본 배포
./deploy.sh --pull         # Git 최신 코드 + 배포
```

### 3. 서비스 상태 확인
```bash
chmod +x health-check.sh
./health-check.sh
```

### 4. Windows 로컬 개발
```cmd
run-local.bat              # 로컬 개발 환경 시작
rebuild-docker.bat         # Docker 재빌드
```

## 📊 접속 정보

- **웹사이트**: http://localhost
- **API 문서**: http://localhost:8080/swagger-ui.html
- **백엔드**: http://localhost:8080
- **모니터링**: `docker-compose logs -f`

## ⚠️ 주의사항

1. `.env` 파일 설정 필수
2. Ubuntu 서버는 최소 4GB RAM 권장
3. 방화벽 포트 80, 443, 8080 개방 필요
4. Docker 설치 후 재로그인 필요