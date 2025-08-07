# 배포 가이드

## 🏠 로컬 개발
```bash
# 개발용 실행
docker-compose -f docker-compose.local.yml up -d

# 또는 직접 실행
cd config-server && ./gradlew bootRun &
./gradlew bootRun
```

## 🚀 운영 배포

### 1. EC2 서버 초기 설정
```bash
# 프로젝트 클론
git clone https://github.com/your-username/byeolnight.git /opt/byeolnight
cd /opt/byeolnight

# 환경변수 설정
cp .env.example .env
nano .env  # 실제 값으로 변경
```

### 2. 배포 방법

#### GitHub에서 자동 배포
```bash
# master 브랜치에 푸시하면 자동 배포
git push origin master
```

#### 수동 배포
```bash
# EC2 서버에서 실행
./scripts/deploy-prod.sh
```

### 3. 설정 암호화
```bash
# 민감한 정보 암호화
./scripts/encrypt-config.sh "암호화할_값"
```

## 📋 파일 구조
- `docker-compose.local.yml` - 로컬 개발용
- `docker-compose.yml` - 운영 배포용
- `.env.example` - 환경변수 템플릿
- `config-repo/` - 설정 파일들 (별도 관리)

## 🔧 트러블슈팅
- Config Server 연결 실패 → 포트 8888 확인
- 배포 실패 → `.env` 파일 확인
- 서비스 상태 → `docker-compose logs -f`