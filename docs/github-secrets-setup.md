# GitHub Secrets 설정 가이드

CI/CD 파이프라인을 위해 다음 Secrets를 GitHub 저장소에 설정해야 합니다.

## 필수 Secrets

### 서버 접속 정보
- `HOST`: EC2 서버 IP 주소 또는 도메인
- `USERNAME`: EC2 서버 사용자명 (예: ubuntu)
- `PRIVATE_KEY`: EC2 서버 접속용 SSH 개인키 (전체 내용)

### GitHub Container Registry
- `GITHUB_TOKEN`: 자동으로 제공됨 (별도 설정 불필요)

## Secrets 설정 방법

1. GitHub 저장소 → Settings → Secrets and variables → Actions
2. "New repository secret" 클릭
3. 각 Secret 추가

## 서버 환경 변수 설정

EC2 서버의 `/home/ubuntu/byeolnight/.env` 파일에 다음 추가:

```bash
# GitHub Container Registry 인증
GITHUB_USERNAME=your-github-username
GITHUB_TOKEN=your-personal-access-token

# Docker 환경 변수
MYSQL_ROOT_PASSWORD=byeolnight9703!@
REDIS_PASSWORD=byeolnight_redis_2025!@
```

## Personal Access Token 생성

1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. "Generate new token (classic)" 클릭
3. 권한 선택:
   - `read:packages`
   - `write:packages`
   - `delete:packages`
4. 생성된 토큰을 서버 `.env` 파일에 추가