# 10. 기여 가이드

> 별 헤는 밤 프로젝트에 기여하고 싶은 개발자들을 위한 가이드입니다.

## 🤝 기여 방법

### 기여할 수 있는 영역

| 영역 | 설명 | 필요 기술 | 난이도 |
|------|------|-----------|--------|
| **🐛 버그 수정** | 기존 기능의 오류 수정 | Java, React, 해당 도메인 지식 | ⭐⭐ |
| **✨ 새 기능 개발** | 로드맵에 있는 기능 구현 | 풀스택 개발 경험 | ⭐⭐⭐ |
| **📚 문서 개선** | README, API 문서, 가이드 작성 | 마크다운, 기술 문서 작성 | ⭐ |
| **🧪 테스트 작성** | 단위/통합 테스트 추가 | JUnit, Mockito, 테스트 경험 | ⭐⭐ |
| **⚡ 성능 최적화** | 쿼리 튜닝, 캐싱 개선 등 | 데이터베이스, 성능 분석 | ⭐⭐⭐⭐ |
| **🔧 인프라 개선** | Docker, CI/CD, 모니터링 | DevOps, 클라우드 경험 | ⭐⭐⭐⭐ |

## 🚀 시작하기

### 1. 개발 환경 설정

```bash
# 1. 저장소 포크 및 클론
git clone https://github.com/your-username/byeolnight.git
cd byeolnight

# 2. 원본 저장소를 upstream으로 추가
git remote add upstream https://github.com/original-owner/byeolnight.git

# 3. 개발 환경 설정
cp .env.example .env
# .env 파일을 개발용 설정으로 수정

# 4. 로컬 환경 실행
./run-local.bat  # Windows
# 또는
./run-local.sh   # Linux/Mac
```

### 2. 브랜치 전략

```bash
# 새 기능 개발
git checkout -b feature/새기능명

# 버그 수정
git checkout -b bugfix/버그설명

# 문서 개선
git checkout -b docs/문서개선내용

# 테스트 추가
git checkout -b test/테스트대상
```

### 3. 커밋 컨벤션

```bash
# 커밋 메시지 형식
<타입>(<범위>): <제목>

<본문>

<푸터>
```

**타입 종류**:
- `feat`: 새로운 기능 추가
- `fix`: 버그 수정
- `docs`: 문서 수정
- `style`: 코드 포맷팅, 세미콜론 누락 등
- `refactor`: 코드 리팩토링
- `test`: 테스트 코드 추가/수정
- `chore`: 빌드 과정 또는 보조 기능 수정

**예시**:
```bash
feat(auth): JWT 토큰 자동 갱신 기능 추가

- Axios 인터셉터를 통한 401 에러 감지
- Refresh Token으로 Access Token 자동 갱신
- 사용자 경험 중단 없이 인증 상태 유지

Closes #123
```

## 📋 기여 프로세스

### 1. 이슈 확인 및 생성

#### 기존 이슈 확인
- [Issues 페이지](https://github.com/your-username/byeolnight/issues)에서 중복 이슈 확인
- `good first issue` 라벨이 있는 이슈는 초보자에게 적합
- `help wanted` 라벨이 있는 이슈는 도움이 필요한 작업

#### 새 이슈 생성
```markdown
## 🐛 버그 리포트 / ✨ 기능 요청

### 설명
간단하고 명확한 설명

### 재현 방법 (버그인 경우)
1. '...' 페이지로 이동
2. '...' 버튼 클릭
3. 오류 발생

### 예상 동작
어떤 동작을 예상했는지 설명

### 실제 동작
실제로 어떤 일이 일어났는지 설명

### 환경
- OS: [예: Windows 11]
- 브라우저: [예: Chrome 120]
- Java 버전: [예: 21]

### 추가 정보
스크린샷, 로그 등 추가 정보
```

### 2. 개발 및 테스트

#### 코드 작성 가이드라인

**Backend (Java/Spring Boot)**:
```java
// 1. 클래스명은 명확하고 의미있게
public class UserAuthenticationService {
    
    // 2. 메서드명은 동작을 명확히 표현
    public TokenResponse authenticateUser(LoginRequest request) {
        // 3. 주석은 '왜'를 설명 ('무엇'이 아닌)
        // 로그인 실패 횟수를 증가시켜 계정 보안을 강화
        increaseLoginFailCount(user);
        
        // 4. 예외 처리는 구체적으로
        throw new AuthenticationException("이메일 또는 비밀번호가 올바르지 않습니다.");
    }
}
```

**Frontend (React/TypeScript)**:
```typescript
// 1. 컴포넌트명은 PascalCase
const UserProfile: React.FC<UserProfileProps> = ({ userId }) => {
  // 2. 커스텀 훅 활용
  const { user, loading, error } = useUser(userId);
  
  // 3. 조기 반환으로 가독성 향상
  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage error={error} />;
  if (!user) return <NotFound />;
  
  // 4. 접근성 고려
  return (
    <div role="main" aria-label="사용자 프로필">
      <h1>{user.nickname}</h1>
      {/* ... */}
    </div>
  );
};
```

#### 테스트 작성

```java
// 단위 테스트 예시
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Test
    @DisplayName("로그인 실패 5회 시 계정이 잠겨야 한다")
    void shouldLockAccountAfterFiveFailedAttempts() {
        // Given
        User user = createTestUser();
        
        // When
        for (int i = 0; i < 5; i++) {
            userService.increaseLoginFailCount(user, "127.0.0.1", "TestAgent");
        }
        
        // Then
        assertThat(user.isAccountLocked()).isTrue();
        assertThat(user.getLoginFailCount()).isEqualTo(5);
    }
}
```

### 3. Pull Request 생성

#### PR 제목 및 설명

```markdown
## 📝 변경 사항 요약
JWT 토큰 자동 갱신 기능 추가

## 🎯 변경 이유
사용자가 게시글 작성 중 토큰 만료로 인한 데이터 손실 문제 해결

## 🔧 주요 변경 내용
- [ ] Axios 인터셉터에 401 에러 처리 로직 추가
- [ ] Refresh Token을 이용한 자동 토큰 갱신 구현
- [ ] 원래 요청 재시도 로직 구현
- [ ] 관련 테스트 코드 추가

## 🧪 테스트 방법
1. 로그인 후 게시글 작성 페이지 이동
2. 30분 대기 (또는 토큰 만료 시간 단축 설정)
3. 게시글 작성 후 저장 버튼 클릭
4. 자동으로 토큰이 갱신되고 게시글이 정상 저장되는지 확인

## 📸 스크린샷 (UI 변경인 경우)
변경 전/후 스크린샷 첨부

## ✅ 체크리스트
- [ ] 코드 리뷰 완료
- [ ] 테스트 코드 작성 및 통과
- [ ] 문서 업데이트 (필요한 경우)
- [ ] 브레이킹 체인지 없음 확인

## 🔗 관련 이슈
Closes #123
```

#### PR 리뷰 과정

1. **자동 검사**: GitHub Actions에서 빌드 및 테스트 실행
2. **코드 리뷰**: 메인테이너가 코드 품질 및 설계 검토
3. **피드백 반영**: 리뷰 의견에 따른 코드 수정
4. **승인 및 머지**: 모든 검토 완료 후 메인 브랜치에 병합

## 🎯 기여 가이드라인

### 코드 품질 기준

#### Backend
- **테스트 커버리지**: 새로운 기능은 반드시 테스트 코드 포함
- **예외 처리**: 모든 예외 상황에 대한 적절한 처리
- **로깅**: 중요한 비즈니스 로직에는 적절한 로그 레벨 적용
- **보안**: 입력값 검증, SQL 인젝션 방지 등 보안 고려

#### Frontend
- **접근성**: WCAG 2.1 AA 수준 준수
- **성능**: 불필요한 리렌더링 방지, 메모이제이션 활용
- **타입 안전성**: TypeScript 타입 정의 철저히
- **사용자 경험**: 로딩 상태, 에러 처리 등 UX 고려

### 문서화 기준

- **API 변경**: Swagger 문서 업데이트 필수
- **새 기능**: README 또는 관련 문서에 사용법 추가
- **설정 변경**: 환경변수나 설정 파일 변경 시 문서 업데이트
- **브레이킹 체인지**: CHANGELOG.md에 명시

## 🏆 기여자 인정

### 기여자 목록
모든 기여자는 README.md의 Contributors 섹션에 추가됩니다.

### 기여 유형별 인정
- **🐛 버그 수정**: Bug Hunter 배지
- **✨ 새 기능**: Feature Developer 배지  
- **📚 문서 개선**: Documentation Expert 배지
- **🧪 테스트 작성**: Test Champion 배지
- **⚡ 성능 최적화**: Performance Optimizer 배지

### 특별 기여자
- **핵심 기여자**: 지속적이고 중요한 기여를 한 개발자
- **멘토**: 다른 기여자들을 도운 개발자
- **리뷰어**: 코드 리뷰에 적극적으로 참여한 개발자

## 📞 소통 채널

### GitHub
- **Issues**: 버그 리포트, 기능 요청
- **Discussions**: 일반적인 질문, 아이디어 논의
- **Pull Requests**: 코드 기여

### 이메일
- **메인테이너 연락**: maintainer@byeolnight.com
- **보안 이슈**: security@byeolnight.com

## ❓ 자주 묻는 질문

### Q: 처음 기여하는데 어떤 작업부터 시작하면 좋을까요?
A: `good first issue` 라벨이 있는 이슈부터 시작하세요. 주로 문서 개선이나 간단한 버그 수정이 포함됩니다.

### Q: 새로운 기능을 제안하고 싶어요.
A: 먼저 GitHub Issues에서 기능 요청 이슈를 생성해주세요. 커뮤니티와 메인테이너의 피드백을 받은 후 개발을 시작하는 것이 좋습니다.

### Q: 코드 리뷰에서 수정 요청을 받았어요.
A: 피드백은 코드 품질 향상을 위한 것입니다. 궁금한 점이 있다면 언제든 질문하세요. 모든 기여자를 환영합니다!

### Q: 테스트 코드 작성이 어려워요.
A: 기존 테스트 코드를 참고하거나, 테스트 작성에 대한 도움이 필요하면 이슈에 `help wanted` 라벨을 추가해주세요.

---

## 🙏 감사 인사

별 헤는 밤 프로젝트에 관심을 가져주셔서 감사합니다. 여러분의 기여가 이 프로젝트를 더욱 발전시킬 것입니다. 

함께 우주의 신비를 탐험하며 멋진 커뮤니티를 만들어가요! 🌟

---

<div align="center">

**🚀 Happy Contributing! 🚀**

</div>