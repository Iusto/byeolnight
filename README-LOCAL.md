# 🌌 로컬 개발 환경 가이드

## 🚀 빠른 시작

### Windows 환경
```bash
# 1. 배포 스크립트 실행
./deploy-local.sh

# 또는 직접 Docker Compose 실행
docker-compose -f docker-compose.local.yml up -d
```

### Linux/Mac 환경
```bash
# 1. 실행 권한 부여
chmod +x deploy-local.sh

# 2. 배포 스크립트 실행
./deploy-local.sh
```

## 🌐 접속 URL

- **프론트엔드**: http://localhost:5173
- **백엔드 API**: http://localhost:8080
- **Config Server**: http://localhost:8888
- **Swagger UI**: http://localhost:8080/swagger-ui.html

## 📋 로그 확인

```bash
# 전체 서비스 로그
docker-compose -f docker-compose.local.yml logs -f

# 특정 서비스 로그
docker logs -f byeolnight-local-app-1
docker logs -f byeolnight-local-config-server-1
```

## 🛑 서비스 중지

```bash
docker-compose -f docker-compose.local.yml down
```

## 🔧 개발 팁

- **빠른 재빌드**: `./gradlew bootJar -x test && docker-compose -f docker-compose.local.yml restart app`
- **데이터 초기화**: `docker-compose -f docker-compose.local.yml down -v`
- **Config 변경 시**: Config Server 재시작 필요