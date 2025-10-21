# 기여 가이드

## 📌 브랜치 전략

### 브랜치 구조
- **main**: 프로덕션 배포 브랜치 (보호됨)
- **develop**: 개발 통합 브랜치 (모든 작업의 시작점)
- **feature/***: 기능 개발 브랜치
- **fix/***: 버그 수정 브랜치
- **hotfix/***: 긴급 수정 브랜치

### 작업 흐름
```bash
# 1. develop에서 작업 브랜치 생성
git checkout develop
git pull origin develop
git checkout -b feature/your-feature

# 2. 작업 후 커밋
git add .
git commit -m "feat: 기능 설명"

# 3. develop에 푸시
git push origin feature/your-feature

# 4. GitHub에서 develop으로 PR 생성
# 5. 모든 체크 통과 후 머지
# 6. develop → main PR은 관리자만 생성
```

### ⚠️ 절대 금지
- **main 브랜치에 직접 푸시 금지**
- **품질 테스트 실패 시 머지 금지**
- **리뷰 없이 머지 금지**

---

## 📝 커밋 컨벤션

### Conventional Commits 사용

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type 종류
- **feat**: 새로운 기능 추가
- **fix**: 버그 수정
- **docs**: 문서 수정
- **style**: 코드 포맷팅 (기능 변경 없음)
- **refactor**: 코드 리팩토링
- **test**: 테스트 코드 추가/수정
- **chore**: 빌드/설정 변경
- **perf**: 성능 개선
- **ci**: CI/CD 설정 변경

### 예시
```bash
# 좋은 예
git commit -m "feat(auth): Google OAuth2 로그인 추가"
git commit -m "fix(user): 닉네임 중복 검사 버그 수정"
git commit -m "refactor(service): UserService 4개로 분리"
git commit -m "test(comment): CommentService 테스트 추가"
git commit -m "docs(readme): 브랜치 전략 문서 추가"

# 나쁜 예
git commit -m "수정"
git commit -m "버그 고침"
git commit -m "작업 완료"
```

### Scope 가이드
- **auth**: 인증/인가
- **user**: 사용자 관리
- **post**: 게시글
- **comment**: 댓글
- **chat**: 채팅
- **message**: 쪽지
- **notification**: 알림
- **api**: API 관련
- **db**: 데이터베이스
- **config**: 설정

---

## ✅ PR 체크리스트

### PR 생성 전 확인사항
- [ ] 모든 테스트 통과 (`gradlew test`)
- [ ] 빌드 성공 (`gradlew build`)
- [ ] 커밋 메시지가 컨벤션 준수
- [ ] 코드 리뷰 가능한 크기 (500줄 이하 권장)
- [ ] 관련 이슈 번호 포함

### PR 템플릿
```markdown
## 변경 사항
- 

## 관련 이슈
- Closes #

## 테스트
- [ ] 단위 테스트 추가
- [ ] 통합 테스트 추가
- [ ] 수동 테스트 완료

## 체크리스트
- [ ] 커밋 컨벤션 준수
- [ ] 모든 테스트 통과
- [ ] 문서 업데이트 (필요시)
```

---

## 🔍 코드 리뷰 가이드

### 리뷰어 체크사항
- 비즈니스 로직 정확성
- 테스트 커버리지
- 보안 취약점
- 성능 이슈
- 코드 가독성

### 리뷰 코멘트 예시
```
✅ LGTM (Looks Good To Me)
💡 제안: ...
❓ 질문: ...
⚠️ 주의: ...
🔴 필수 수정: ...
```

---

## 🚀 배포 프로세스

### develop → main 머지 조건
1. ✅ 모든 GitHub Actions 워크플로우 통과
   - CI 테스트
   - 코드 품질 검사
   - 보안 스캔
2. ✅ 최소 1명 이상 승인
3. ✅ 충돌 없음
4. ✅ 모든 대화 해결됨

### 자동 체크 항목
- Build & Test
- Code Quality (CodeQL)
- Security Scan (OWASP)
- Dependency Check
- Test Coverage

---

## 📚 추가 문서
- [테스트 전략](./docs/07_testing.md)
- [아키텍처 구조](./docs/03_architecture.md)
- [코딩 컨벤션](./docs/coding-conventions.md)
