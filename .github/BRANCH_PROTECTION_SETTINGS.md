# GitHub 브랜치 보호 설정 가이드

## main 브랜치 보호 규칙

GitHub 저장소 Settings → Branches → Add rule에서 다음 설정:

### Branch name pattern
```
main
```

### 필수 설정
- [x] **Require a pull request before merging**
  - [x] Require approvals: 1
  - [x] Dismiss stale pull request approvals when new commits are pushed
  - [x] Require review from Code Owners (선택)

- [x] **Require status checks to pass before merging**
  - [x] Require branches to be up to date before merging
  - 필수 체크:
    - `check-branch`
    - `commit-convention`
    - `quality-gate`
    - `build`
    - `test`

- [x] **Require conversation resolution before merging**

- [x] **Require linear history** (선택)

- [x] **Do not allow bypassing the above settings**

- [x] **Restrict who can push to matching branches**
  - 관리자만 직접 푸시 가능

---

## develop 브랜치 보호 규칙

### Branch name pattern
```
develop
```

### 필수 설정
- [x] **Require a pull request before merging**
  - [x] Require approvals: 1

- [x] **Require status checks to pass before merging**
  - 필수 체크:
    - `quality-gate`
    - `build`
    - `test`

- [x] **Require conversation resolution before merging**

---

## 설정 후 확인사항

### 테스트 시나리오
1. ✅ feature 브랜치 → develop PR 생성 가능
2. ✅ develop → main PR 생성 가능
3. ❌ feature 브랜치 → main PR 생성 불가
4. ❌ main 브랜치 직접 푸시 불가
5. ✅ 테스트 실패 시 머지 불가
6. ✅ 커밋 컨벤션 위반 시 머지 불가

### 워크플로우 확인
```bash
# 로컬에서 테스트
./gradlew test
./gradlew build

# 커밋 메시지 검증
git log --oneline -5
# 모두 "type(scope): subject" 형식이어야 함
```

---

## 긴급 상황 대응

### Hotfix 프로세스
```bash
# main에서 hotfix 브랜치 생성
git checkout main
git checkout -b hotfix/critical-bug

# 수정 후 커밋
git commit -m "fix(critical): 긴급 버그 수정"

# main과 develop 모두에 PR 생성
git push origin hotfix/critical-bug
```

### 브랜치 보호 임시 해제
- Settings → Branches → Edit rule
- "Allow force pushes" 체크 (작업 후 즉시 해제)
- 반드시 팀원에게 공지 후 진행
