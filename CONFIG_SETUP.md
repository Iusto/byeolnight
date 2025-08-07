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

# Config Repository 클론 (별도 Private Repository)
git clone https://github.com/your-username/byeolnight-config.git config-repo
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

### 3. 설정 변경
```bash
# Config Repository에서 설정 변경
cd config-repo
nano configs/byeolnight-prod.yml
git add . && git commit -m "Update config" && git push

# 애플리케이션에서 설정 새로고침
curl -X POST -u admin:password http://localhost:8080/api/admin/config/refresh
```

## 📋 파일 구조
- `docker-compose.local.yml` - 로컬 개발용
- `docker-compose.yml` - 운영 배포용
- `config-repo/` - 설정 파일들 (별도 Private Repository)
- `scripts/encrypt-config.sh` - 설정 암호화 도구

## 🔧 설정 암호화
```bash
# 민감한 정보 암호화
./scripts/encrypt-config.sh "암호화할_값"

# 결과를 config-repo/configs/byeolnight-prod.yml에 추가
property: '{cipher}암호화된값'
```

## 🔧 트러블슈팅
- Config Server 연결 실패 → 포트 8888 확인
- 배포 실패 → `docker-compose logs -f`
- 설정 로드 실패 → Config Repository 경로 확인