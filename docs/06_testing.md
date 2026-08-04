# 06. 테스트 전략

> 운영급 코드 품질 보장을 위한 실전 테스트 전략

## 🧪 테스트 아키텍처

```
    ┌─────────────────────────────────────────┐
    │       Service Tests (157개)             │  ← 핵심 비즈니스 로직
    │  Mock 기반 격리 테스트                    │     100% 통과 ✅
    └─────────────────────────────────────────┘
      ┌───────────────────────────────────────┐
      │     Controller Tests (37개)           │  ← API 엔드포인트
      │  @WebMvcTest + TestSecurityConfig     │     100% 통과 ✅
      └───────────────────────────────────────┘
        ┌─────────────────────────────────────┐
        │     Repository Tests (33개)         │  ← QueryDSL 동적 쿼리, 커서 페이징
        │  @DataJpaTest + H2 실 DB 검증       │     100% 통과 ✅
        └─────────────────────────────────────┘
          ┌───────────────────────────────────┐
          │  Infrastructure (9개) / 통합 (6개) │  ← 설정·보안, Embedded Redis
          └───────────────────────────────────┘
```

**전체: 242개 테스트, 실패 0건 / 스킵 0건** (2026-08-04 기준, `./gradlew test`)

> 소스의 `@Test`/`@ParameterizedTest` 선언은 222개이고, `@ParameterizedTest`가
> 파라미터별로 펼쳐지면서 실제 실행 건수는 242개가 된다. 테스트 클래스는 33개다.

---

## 📋 테스트 전략

| 계층 | 상태 | 테스트 방식 | 주요 검증 항목 |
|------|------|-------------|----------------|
| **Service** | ✅ 완료 (157개) | Mock + Lenient 모드 | 비즈니스 로직, 예외 처리, 보안 정책 |
| **Controller** | ✅ 완료 (37개) | @WebMvcTest + MockMvc | HTTP 상태코드, 인증/인가, 응답 구조 |
| **Repository** | ✅ 완료 (33개) | @DataJpaTest + H2 | QueryDSL 동적 쿼리, 필터링, 커서 페이징 |
| **Infrastructure** | ✅ 완료 (9개) | 단위 + Embedded Redis | 설정, 보안 필터, IP 차단 |
| **통합** | ✅ 완료 (6개) | SpringBootTest | 주요 플로우 |
| **Frontend** | 🔲 미구현 | - | 빌드 성공 여부만 CI에서 체크 중 |

---

## 🔍 계층별 테스트 상세

### 1. Service 레이어 테스트 (157개)

Mock 기반으로 외부 의존성(DB, Redis, 외부 API)을 격리하여 비즈니스 로직만 검증한다.

**주요 테스트 클래스:**

| 클래스 | 테스트 수 | 주요 검증 |
|--------|--------|----------|
| `IssServiceTest` | 18개 | SGP4 궤도 계산, 방위각 변환(14 케이스), 캐싱, 폴백 처리 |
| `TokenServiceTest` | 17개 | Redis 토큰 관리, 블랙리스트, TTL, 해시 검증 |
| `EmailAuthServiceTest` | 12개 | 이메일 인증, 시도 횟수 제한, 코드 검증 |
| `CoordinateUtilsTest` | 9개 | 좌표 유틸리티 변환 정확성 |
| `SocialAccountCleanupServiceTest` | 8개 | 30일 내 복구, 개인정보 마스킹 |
| `WeatherServiceTest` | 7개 | 날씨 API 파싱, 캐싱, 에러 처리 |
| `SchedulerServiceTest` | 7개 | 크론 표현식, 자동화 작업 실행 |
| `SchedulerCronExpressionTest` | 7개 | 크론 주기 검증 |
| `NotificationServiceTest` | 6개 | 알림 생성/조회/읽음 처리 |
| `UserAccountServiceTest` | 5개 | 비밀번호 변경, 탈퇴, 계정 복구 |
| `UserQueryServiceTest` | 5개 | 사용자 조회, 중복 검사 |
| `CommentServiceTest` | 5개 | 댓글 CRUD, 권한 검증 |
| `AuthServiceTest` | 4개 | 로그인 성공/실패, 5회 잠금, IP 차단 |
| `MessageServiceTest` | 4개 | 쪽지 전송, 수신함 조회 |
| `PointServiceAttendanceTest` | 4개 | 출석 포인트 지급, 중복 방지 |
| `PostServiceBlindTest` | 3개 | 게시글 블라인드 처리 |
| `UserAdminServiceTest` | 3개 | 관리자 계정 복구 |
| `SchedulerUnitTest` | 6개 | 스케줄러 단위 실행 검증 |
| `WeatherSchedulerTest` | 5개 | 날씨 스케줄 동작 |
| `CustomOAuth2UserServiceWithdrawTest` | 2개 | 소셜 탈퇴 계정 처리 |

**대표 예시:**

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Test
    @DisplayName("5회 실패 후 로그인 시도 - 경고 메시지 포함")
    void authenticate_FifthFailure_ShowsWarningMessage() {
        User userWith4Failures = testUser.toBuilder()
                .loginFailCount(4)
                .build();

        assertThatThrownBy(() -> authService.authenticate(wrongPasswordRequest, request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("⚠️ 경고")
                .hasMessageContaining("5회 더 틀리면 계정이 잠깁니다");
    }
}
```

---

### 2. Controller 레이어 테스트 (37개)

`@WebMvcTest`로 컨트롤러 슬라이스만 로딩하고, MockMvc로 HTTP 요청/응답을 검증한다.
JWT 필터를 제외하고 URL 기반 보안만 적용하는 `TestSecurityConfig`를 공유 설정으로 사용한다.

**테스트 구성:**

| 클래스 | 테스트 수 | 주요 검증 |
|--------|--------|----------|
| `AuthControllerTest` | 12개 | 로그인(200/401/403), 로그아웃, 인증 확인, 이메일 인증 |
| `MemberPostControllerTest` | 11개 | 게시글 CRUD 권한 (비로그인→401, 로그인→200), 입력값 검증 |
| `PublicPostControllerTest` | 5개 | 공개 게시글 목록/상세/인기글 조회 |
| `ChatControllerTest` | 6개 | 채팅 이력 조회, 채팅 금지 상태 확인 |

**보안 설정 구조:**

```java
// @WebMvcTest 공통 설정
@WebMvcTest(
    value = AuthController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
    }
)
@Import(TestSecurityConfig.class)
class AuthControllerTest { ... }
```

**대표 예시:**

```java
@Test
@DisplayName("잘못된 비밀번호 시 401 반환")
void wrongPassword_returns401() throws Exception {
    when(authService.authenticate(any(), any()))
            .thenThrow(new BadCredentialsException("비밀번호가 올바르지 않습니다."));

    mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false));
}
```

---

### 3. Repository 레이어 테스트 (33개)

`@DataJpaTest`로 JPA 슬라이스만 로딩하고, H2 인메모리 DB에 실제 쿼리를 실행하여 QueryDSL 동적 쿼리를 검증한다.

**설정:**
- `@ActiveProfiles("test")` + `@AutoConfigureTestDatabase(replace = NONE)` → `application-test.yml`의 H2 설정 사용
- `@Import(QueryDslConfig.class)` → `JPAQueryFactory` 빈 제공 (QueryDSL 필수)
- H2 URL에 `NON_KEYWORDS=USER` 설정 필수 (`user` 테이블명이 H2 예약어)

**테스트 구성:**

| 클래스 | 테스트 수 | 주요 검증 |
|--------|--------|----------|
| `PostRepositoryTest` | 20개 | 카테고리 필터, 삭제 제외, QueryDSL 키워드 검색, HOT 게시글, 블라인드, 페이징, 게시글 수 |
| `UserRepositoryTest` | 11개 | 이메일 조회, 닉네임 중복, 탈퇴 상태 조회, 소셜 탈퇴 경과 사용자 |
| `ChatMessageRepositoryTest` | 2개 | 커서 페이지네이션 중복·누락 없음, 정렬 기준이 커서와 동일한지 |

**대표 예시 — 커서 페이지네이션:**

`id`와 `timestamp`의 순서가 어긋난 데이터를 만들고 첫 페이지부터 끝까지 넘겨,
중복·누락 없이 전부 조회되는지 검증한다. `@CreatedDate` 감사가 저장 시각을 덮어쓰므로
저장 후 JPQL로 시각을 조정해 역전 상황을 만든다.
자세한 배경은 [채팅 커서 페이지네이션 수정](./16_chat-cursor-pagination.md) 참조.

**대표 예시 — QueryDSL 복합 조건 검색:**

```java
@Test
@DisplayName("카테고리 필터와 키워드 동시 적용 시 교집합 반환")
void shouldApplyCategoryAndKeywordTogether() {
    savePost("우주 탐사", Post.Category.FREE);
    savePost("우주 뉴스", Post.Category.NEWS);
    savePost("날씨 정보", Post.Category.FREE);

    Page<Post> result = postRepository.searchPosts("우주", Post.Category.FREE, "title",
            PageRequest.of(0, 10));

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getTitle()).isEqualTo("우주 탐사");
}
```

---

## 🛠️ 공통 테스트 설정

### TestSecurityConfig

Controller 테스트 공유 보안 설정. JWT 필터 없이 URL 기반 인증만 적용한다.

```java
@TestConfiguration
public class TestSecurityConfig {
    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(AuthWhitelist.PATHS).permitAll()
                .requestMatchers("/api/member/**").authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint()));
        return http.build();
    }
}
```

### TestMockConfig

Service 테스트 공유 Mock 설정. `HttpServletRequest` 헤더 13개를 일괄 설정한다.

```java
public class TestMockConfig {
    public static void setupHttpServletRequest(HttpServletRequest request) {
        given(request.getHeader("X-Client-IP")).willReturn(TEST_IP);
        given(request.getHeader("X-Forwarded-For")).willReturn(null);
        given(request.getRemoteAddr()).willReturn(TEST_IP);
        // ... 모든 필수 헤더 설정
    }
}
```

---

## 🔥 부하테스트 (k6)

### 테스트 환경

| 항목 | 설정 |
|------|------|
| 부하 생성 서버 | 별도 EC2 t3.micro (애플리케이션 서버와 분리) |
| 대상 서버 | 운영 EC2 (Spring Boot + 로컬 캐시) |
| 도구 | k6 (Grafana Labs) |
| 시나리오 | 150 VU, 2분 30초 (ramp-up 30초 → 유지 2분) |
| 검증 | Actuator `/actuator/metrics/cache.gets` 캐시 히트/미스 정량 검증 |

### Weather 캐시 테스트 결과

| 지표 | 결과 | 임계값 | 판정 |
|------|------|--------|------|
| 총 요청 | 316,953건 | - | - |
| 에러율 | 0% | < 1% | PASS |
| p(95) 응답시간 | 49.29ms | < 50ms | PASS |
| 초당 처리량 | 2,112 req/s | - | - |
| 캐시 적중률 | 100% | - | - |

### ISS 캐시 테스트 결과

| 지표 | 결과 | 임계값 | 판정 |
|------|------|--------|------|
| 총 요청 | 290,792건 | - | - |
| 에러율 | 0% | < 1% | PASS |
| p(95) 응답시간 | 51.15ms | < 50ms | FAIL* |
| 캐시 적중률 | 99.995% | - | - |

> *miss 14건은 SGP4 계산 캐시 TTL(2시간) 만료 시점에 발생한 정상적인 캐시 미스. p(95) 임계값을 60ms로 조정하거나 Proactive 계산으로 개선 가능.

---

## 🎯 테스트 실행 명령어

```bash
# 전체 테스트 (242개)
./gradlew test

# 계층별 실행
./gradlew test --tests "com.byeolnight.service.*"
./gradlew test --tests "com.byeolnight.controller.*"
./gradlew test --tests "com.byeolnight.repository.*"

# 특정 클래스
./gradlew test --tests "com.byeolnight.service.auth.AuthServiceTest"
./gradlew test --tests "com.byeolnight.repository.post.PostRepositoryTest"
```

### 최신 테스트 실행 결과 (2026-08-04)

```
> Task :test
BUILD SUCCESSFUL

242 tests completed, 0 failed, 0 skipped ✅
```

---

## 🚀 다음 단계

- **Frontend 단위 테스트**: Vitest 기반 훅/컴포넌트 테스트 추가 (현재 빌드 체크만 존재)
- **E2E 테스트**: 주요 사용자 플로우 전체 검증

---

👉 다음 문서: [07. 배포 가이드](./08_deployment.md)
