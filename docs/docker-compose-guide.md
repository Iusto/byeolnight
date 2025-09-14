# Docker Compose 파일 가이드

## 파일별 역할

### 1. `docker-compose.yml` (기존 운영용)
- **용도**: 기존 방식의 운영 배포 (서버에서 직접 빌드)
- **특징**: 
  - 소스코드를 서버에서 빌드
  - Nginx + SSL 인증서 포함
  - 프론트엔드 빌드 포함
- **사용법**: `docker-compose up -d`

### 2. `docker-compose.local.yml` (로컬 개발용)
- **용도**: 로컬 개발 환경
- **특징**:
  - Config Server + MySQL + Redis만 실행
  - 메인 앱은 IDE에서 직접 실행
  - 개발용 설정 (메모리 사용량 최소화)
- **사용법**: `docker-compose -f docker-compose.local.yml up -d`

### 3. `docker-compose.prod.yml` (CI/CD용 운영)
- **용도**: CI/CD 파이프라인 기반 운영 배포
- **특징**:
  - 미리 빌드된 이미지 사용
  - 무중단 배포 지원
  - 동적 포트 설정 가능
- **사용법**: `docker-compose -f docker-compose.prod.yml up -d`

## 권장 사용 시나리오

### 로컬 개발
```bash
docker-compose -f docker-compose.local.yml up -d
# IDE에서 메인 애플리케이션 실행
```

### 기존 방식 운영 배포
```bash
./deploy.sh  # docker-compose.yml 사용
```

### CI/CD 기반 운영 배포
```bash
./deploy-zero-downtime.sh  # docker-compose.prod.yml 사용
```