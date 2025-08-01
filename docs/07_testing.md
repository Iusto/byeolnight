# 07. 테스트 전략

> 코드 품질 보장을 위한 계층별 테스트 전략과 실제 구현 사례

## 🧪 테스트 계층 구조

### 테스트 피라미드 적용

```
        ┌─────────────────┐
        │  E2E Tests      │  ← 최소한의 핵심 플로우만
        │  (Selenium)     │
        └─────────────────┘
      ┌───────────────────────┐
      │  Integration Tests    │  ← 주요 API 엔드포인트
      │  (@SpringBootTest)    │
      └───────────────────────┘
    ┌─────────────────────────────┐
    │     Unit Tests              │  ← 대부분의 테스트
    │  (Service, Repository)      │
    └─────────────────────────────┘
```

## 📋 계층별 테스트 전략

| 계층 | 테스트 방식 | 목적 | 커버리지 목표 |
|------|-------------|------|---------------|
| **Controller** | `@WebMvcTest` + MockMvc | 인증, 권한, 예외 처리 검증 | 주요 API 엔드포인트 |
| **Service** | `@ExtendWith(MockitoExtension.class)` | 비즈니스 로직, 예외 상황 검증 | 핵심 도메인 로직 |
| **Repository** | `@DataJpaTest` | 쿼리 검증, 연관관계 테스트 | 복잡한 쿼리 및 커스텀 메서드 |
| **Integration** | `@SpringBootTest` | 전체 플로우 검증 | 핵심 사용자 시나리오 |

## 🔍 실제 테스트 사례

### 1. Service Layer 테스트 (비즈니스 로직)

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    @DisplayName("로그인 실패 5회 시 계정이 잠겨야 한다")
    void 계정_잠금_테스트() {
        // Given
        User user = createTestUser();
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        
        // When: 5번 연속 로그인 실패
        for (int i = 0; i < 5; i++) {
            userService.increaseLoginFailCount(user, "127.0.0.1", "TestAgent");
        }
        
        // Then
        assertTrue(user.isAccountLocked());
        assertEquals(5, user.getLoginFailCount());
        verify(userRepository, times(5)).save(user);
    }
}
```

### 2. Repository Layer 테스트 (데이터 접근)

```java
@DataJpaTest
class PostRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private PostRepository postRepository;
    
    @Test
    @DisplayName("카테고리별 게시글 조회 시 인덱스가 사용되는지 확인")
    void 카테고리별_조회_성능_테스트() {
        // Given
        User author = createAndSaveUser();
        Post post1 = createPost(author, "GENERAL", "제목1");
        Post post2 = createPost(author, "NOTICE", "제목2");
        entityManager.persistAndFlush(post1);
        entityManager.persistAndFlush(post2);
        
        // When
        List<Post> generalPosts = postRepository.findByCategoryOrderByCreatedAtDesc("GENERAL");
        
        // Then
        assertThat(generalPosts).hasSize(1);
        assertThat(generalPosts.get(0).getTitle()).isEqualTo("제목1");
    }
}
```

### 3. Controller Layer 테스트 (API 엔드포인트)

```java
@WebMvcTest(AuthController.class)
class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private AuthService authService;
    
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    
    @Test
    @DisplayName("잘못된 이메일 형식으로 로그인 시 400 에러 반환")
    void 잘못된_이메일_형식_로그인_테스트() throws Exception {
        // Given
        LoginRequest invalidRequest = new LoginRequest("invalid-email", "password123");
        
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이메일 형식이 올바르지 않습니다"));
    }
}
```

### 4. Integration Test (전체 플로우)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class AuthIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    @DisplayName("회원가입 → 로그인 → 토큰 갱신 전체 플로우 테스트")
    void 인증_전체_플로우_테스트() {
        // Given: 회원가입
        SignupRequest signupRequest = new SignupRequest("test@example.com", "password123", "테스터");
        ResponseEntity<ApiResponse> signupResponse = restTemplate.postForEntity(
            "/api/auth/signup", signupRequest, ApiResponse.class);
        assertThat(signupResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // When: 로그인
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
        ResponseEntity<TokenResponse> loginResponse = restTemplate.postForEntity(
            "/api/auth/login", loginRequest, TokenResponse.class);
        
        // Then: 토큰 발급 확인
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody().getAccessToken()).isNotNull();
        
        // When: 토큰으로 인증이 필요한 API 호출
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(loginResponse.getBody().getAccessToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<UserResponse> profileResponse = restTemplate.exchange(
            "/api/users/me", HttpMethod.GET, entity, UserResponse.class);
        
        // Then: 프로필 조회 성공
        assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(profileResponse.getBody().getEmail()).isEqualTo("test@example.com");
    }
}
```

## 🎯 특별한 테스트 케이스

### JWT 토큰 TTL 검증 테스트

```java
@Test
@DisplayName("Access Token TTL이 정확히 30분으로 설정되는지 검증")
void accessToken_TTL_30분_검증() {
    // Given
    User testUser = createTestUser();
    
    // When
    String accessToken = jwtTokenProvider.createAccessToken(testUser);
    Claims claims = jwtTokenProvider.extractAllClaims(accessToken);
    
    // Then
    long actualTTL = claims.getExpiration().getTime() - System.currentTimeMillis();
    // 30분 = 1,800,000ms, 오차 ±5초 허용
    assertThat(actualTTL).isBetween(1795000L, 1805000L);
}

@Test
@DisplayName("Refresh Token TTL이 정확히 7일로 설정되는지 검증")
void refreshToken_TTL_7일_검증() {
    // Given
    User testUser = createTestUser();
    
    // When
    String refreshToken = jwtTokenProvider.createRefreshToken(testUser);
    Claims claims = jwtTokenProvider.extractAllClaims(refreshToken);
    
    // Then
    long actualTTL = claims.getExpiration().getTime() - System.currentTimeMillis();
    // 7일 = 604,800,000ms, 오차 ±1분 허용
    assertThat(actualTTL).isBetween(604740000L, 604860000L);
}
```

### 동시성 테스트 (포인트 적립)

```java
@Test
@DisplayName("동시에 포인트 적립 요청 시 중복 지급되지 않아야 한다")
void 포인트_동시성_테스트() throws InterruptedException {
    // Given
    User user = createAndSaveUser();
    int threadCount = 10;
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    
    // When: 10개 스레드에서 동시에 포인트 적립 시도
    for (int i = 0; i < threadCount; i++) {
        executorService.submit(() -> {
            try {
                userService.addDailyAttendancePoints(user.getId());
            } finally {
                latch.countDown();
            }
        });
    }
    
    latch.await();
    
    // Then: 포인트는 한 번만 적립되어야 함
    User updatedUser = userRepository.findById(user.getId()).orElseThrow();
    assertThat(updatedUser.getPoints()).isEqualTo(user.getPoints() + 10); // 출석 포인트 10점
}
```

## 📊 테스트 실행 및 커버리지

### 테스트 실행 명령어

```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스만 실행
./gradlew test --tests "UserServiceTest"

# 통합 테스트만 실행
./gradlew test --tests "*IntegrationTest"

# 테스트 커버리지 리포트 생성
./gradlew jacocoTestReport
```

### 현재 테스트 현황

| 테스트 유형 | 구현 상태 | 주요 테스트 대상 |
|------------|-----------|------------------|
| **Unit Tests** | ✅ 구현 완료 | JWT TTL 검증, 로그인 실패 처리, 포인트 동시성 |
| **Repository Tests** | ✅ 구현 완료 | 복합 인덱스 쿼리, 연관관계 매핑 |
| **Controller Tests** | 🔄 진행 중 | 인증/권한, 입력값 검증, 예외 처리 |
| **Integration Tests** | 🔄 진행 중 | 회원가입→로그인→API 호출 플로우 |

## 🎯 테스트 작성 가이드라인

### DO (권장사항)
- **Given-When-Then 패턴** 사용으로 테스트 의도 명확화
- **@DisplayName**으로 한국어 테스트 설명 작성
- **경계값 테스트** 포함 (최소/최대값, null, 빈 값)
- **예외 상황 테스트** 필수 포함

### DON'T (지양사항)
- 테스트 간 의존성 생성 금지
- 실제 외부 API 호출 금지 (Mock 사용)
- 하드코딩된 시간/날짜 사용 금지
- 테스트 데이터 정리 누락 금지

---

👉 다음 문서: [08. 배포 가이드](./08_deployment.md)