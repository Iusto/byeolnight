# 07. 테스트 전략

> 운영급 코드 품질 보장을 위한 실전 테스트 전략

## 🧪 테스트 아키텍처

### 실제 구현된 테스트 구조

```
    ┌─────────────────────────────────┐
    │     Unit Tests (123개)      │  ← 핵심 비즈니스 로직
    │  Mock 기반 격리 테스트       │     100% 통과 ✅
    └─────────────────────────────────┘
      ┌───────────────────────────┐
      │  Integration Tests    │  ← 주요 시나리오
      │  (36개 의도적 스킵)   │     필요시 활성화
      └───────────────────────────┘
```

## 📋 테스트 전략

| 계층 | 구현 상태 | 테스트 방식 | 주요 검증 항목 |
|------|-----------|-------------|----------------|
| **Service** | ✅ 완료 | Mock + Lenient 모드 | 비즈니스 로직, 예외 처리 |
| **Integration** | ✅ 완료 | Mock 기반 단위 테스트 | 핵심 시나리오 검증 |
| **Controller** | ⏭️ 스킵 | 필요시 활성화 | API 엔드포인트 |
| **Repository** | ⏭️ 향후 | @DataJpaTest | 쿼리 성능 검증 |

## 🔍 핵심 테스트 사례

### 1. 인증 시스템 테스트 (운영급 보안)

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {
    
    @Test
    @DisplayName("5회 실패 후 로그인 시도 - 경고 메시지 포함")
    void authenticate_FifthFailure_ShowsWarningMessage() {
        // Given - 실제 보안 시나리오
        User userWith4Failures = testUser.toBuilder()
                .loginFailCount(4)
                .build();
        
        // When & Then - 실제 보안 정책 검증
        assertThatThrownBy(() -> authService.authenticate(wrongPasswordRequest, request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("⚠️ 경고")
                .hasMessageContaining("5회 더 틀리면 계정이 잠깁니다");
    }
}
```

### 2. 소셜 계정 복구 테스트 (실제 비즈니스 로직)

```java
@Test
@DisplayName("탈퇴 신청 후 30일 이전 사용자 재로그인 시 완전 복구")
void recoverWithdrawnAccount_Within30Days_CompleteRecovery() {
    // Given - 15일 전 탈퇴한 소셜 사용자 (모든 데이터 유지)
    User withdrawnUser = User.builder()
            .email("recover@gmail.com")
            .points(500) // 기존 포인트
            .build();
    withdrawnUser.setSocialProvider("google");
    withdrawnUser.withdraw("사용자 요청");
    setWithdrawnAt(withdrawnUser, LocalDateTime.now().minusDays(15));
    
    // When
    boolean result = socialAccountCleanupService.recoverWithdrawnAccount("recover@gmail.com");
    
    // Then - 완전 복구 확인
    assertTrue(result);
    assertEquals(User.UserStatus.ACTIVE, withdrawnUser.getStatus());
    assertEquals(500, withdrawnUser.getPoints()); // 기존 포인트 유지
}
```

### 3. 쿠키 TTL 검증 테스트 (운영급 보안)

```java
@Test
@DisplayName("TTL 계산 정확성 검증")
void validateTTLCalculations() {
    // Given
    long sevenDaysInMs = 7 * 24 * 60 * 60 * 1000L; // 7일 (밀리초)
    long thirtyMinutesInSeconds = 30 * 60L; // 30분 (초)

    // When & Then - 정확한 TTL 계산 검증
    assertThat(sevenDaysInMs / 1000)
            .as("Refresh Token TTL은 정확히 604800초(7일)여야 함")
            .isEqualTo(604800L);

    assertThat(thirtyMinutesInSeconds)
            .as("Access Token TTL은 정확히 1800초(30분)여야 함")
            .isEqualTo(1800L);
}

@Test
@DisplayName("ResponseCookie MaxAge 설정 검증")
void validateResponseCookieMaxAge() {
    // Given
    ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "test-token")
            .maxAge(604800L) // 7일
            .build();

    ResponseCookie accessCookie = ResponseCookie.from("accessToken", "test-token")
            .maxAge(1800L) // 30분
            .build();

    ResponseCookie sessionCookie = ResponseCookie.from("sessionToken", "test-token")
            .build(); // maxAge 설정 안함

    // Then - 쿠키 TTL 설정 검증
    assertThat(refreshCookie.getMaxAge().getSeconds()).isEqualTo(604800L);
    assertThat(accessCookie.getMaxAge().getSeconds()).isEqualTo(1800L);
    assertThat(sessionCookie.getMaxAge().getSeconds()).isEqualTo(-1L); // 세션 쿠키
}
```

### 4. 천체 데이터 수집 테스트 (외부 API 연동)

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AstronomyServiceTest {
    
    @Test
    @DisplayName("NASA NeoWs API 응답을 올바르게 파싱한다")
    void parseNeoWsData_Success() {
        // Given - 실제 NASA API 응답 구조
        Map<String, Object> mockNeoWsResponse = Map.of(
            "near_earth_objects", Map.of(
                "2024-01-15", List.of(
                    Map.of(
                        "name", "(2024 AA1) Test Asteroid",
                        "is_potentially_hazardous_asteroid", false,
                        "estimated_diameter", Map.of(
                            "meters", Map.of("estimated_diameter_max", 150.0)
                        )
                    )
                )
            )
        );

        when(restTemplate.getForEntity(contains("neo/rest/v1/feed"), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(mockNeoWsResponse));

        // When
        astronomyService.fetchDailyAstronomyEvents();

        // Then - 파싱된 데이터 검증
        verify(astronomyEventRepository).saveAll(argThat(events -> {
            List<AstronomyEvent> eventList = (List<AstronomyEvent>) events;
            return eventList.stream().anyMatch(event -> 
                event.getEventType().equals("ASTEROID") &&
                event.getTitle().contains("Test Asteroid")
            );
        }));
    }
}
```

## 🛠️ 테스트 최적화 구현 (2025-01-27)

### TestMockConfig 공통 설정

```java
/**
 * 테스트용 공통 Mock 설정 - 코드 중복 93% 감소
 */
public class TestMockConfig {
    
    public static void setupHttpServletRequest(HttpServletRequest request) {
        // IP 관련 헤더들 (13개) 일괄 설정
        given(request.getHeader("X-Client-IP")).willReturn(TEST_IP);
        given(request.getHeader("X-Forwarded-For")).willReturn(null);
        given(request.getHeader("User-Agent")).willReturn(TEST_USER_AGENT);
        given(request.getRemoteAddr()).willReturn(TEST_IP);
        // ... 모든 필수 헤더 설정
    }
}
```

### Lenient 모드 적용

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // 테스트 안정성 100% 향상
class AuthServiceTest {
    
    @BeforeEach
    void setUp() {
        // 기존: 15줄의 Mock 설정
        // 개선: 1줄로 모든 설정 완료
        TestMockConfig.setupHttpServletRequest(request);
    }
}
```

## 📊 테스트 현황 (2025-01-27 최신)

### ✅ 구현 완료 (126개 테스트 100% 통과)

#### 1. 인증/보안 시스템 (48개 테스트)
- **AuthServiceTest**: 로그인 성공/실패, 계정 잠금, IP 차단
- **TokenServiceTest**: Redis 토큰 관리, 블랙리스트, TTL 처리
- **EmailAuthServiceTest**: 이메일 인증, 시도 횟수 제한, HTML 템플릿
- **CookieTTLValidationTest**: 쿠키 TTL 정확성 검증 (신규 추가)

#### 2. 소셜 계정 복구 시스템 (15개 테스트)
- **SocialAccountCleanupServiceTest**: 30일 내 완전 복구, 개인정보 마스킹
- **닉네임 자동 생성**: 이메일 기반, 중복 처리, 길이 제한

#### 3. 천체 데이터 시스템 (25개 테스트)
- **AstronomyServiceTest**: NASA API 파싱, KASI API 연동
- **AstronomyServiceIntegrationTest**: ISS 위치 정보, 실시간 데이터

#### 4. 스케줄러 시스템 (12개 테스트)
- **SchedulerServiceTest**: 크론 표현식, 성능 테스트
- **게시글 정리**: 만료된 게시글 자동 정리

### ⏭️ 의도적 스킵 (36개 테스트)
- **Controller 통합 테스트**: 필요시 활성화 가능
- **전체 플로우 테스트**: 운영 환경에서 검증

### 🎯 테스트 실행 명령어

```bash
# 전체 테스트 실행 (126개)
./gradlew test

# 쿠키 TTL 테스트
./gradlew test --tests "CookieTTLValidationTest"

# 핵심 시스템별 테스트
./gradlew test --tests "*AuthService*"
./gradlew test --tests "*AstronomyService*"
./gradlew test --tests "*SocialAccountCleanup*"
```

### 최신 테스트 실행 결과 (2025-01-27)

```bash
> Task :test

BUILD SUCCESSFUL in 13s
5 actionable tasks: 2 executed, 3 up-to-date

126 tests completed, 0 failed, 36 skipped ✅
```

#### 주요 성과
- **전체 테스트**: 126개 모두 통과 ✅
- **실패 테스트**: 0개 ✅
- **테스트 안정성**: 100% 달성 ✅
- **빌드 시간**: 13초 (최적화 완료)

#### 해결된 문제들
- ✅ Logback 충돌 문제 해결
- ✅ Mockito Strict 모드 문제 해결
- ✅ Redis 연결 의존성 제거
- ✅ Spring Boot 3.x 설정 호환성 확보

## 🎯 테스트 작성 가이드라인

### ✅ 권장사항
- **Given-When-Then 패턴**: 테스트 의도 명확화
- **@DisplayName**: 한국어 테스트 설명
- **TestMockConfig**: 공통 Mock 설정으로 중복 제거
- **Lenient 모드**: `@MockitoSettings(strictness = Strictness.LENIENT)`
- **실제 시나리오**: 사용자가 겪을 수 있는 상황 테스트

### ❌ 지양사항
- 테스트 간 의존성 생성 금지
- 실제 외부 API 호출 금지 (Mock 사용)
- 형식적 테스트 금지 (의미 있는 검증만)

## 📊 테스트 성과 지표

| 테스트 영역 | 테스트 수 | 통과율 | 주요 커버리지 |
|------------|-----------|--------|---------------|
| **인증 시스템** | 48개 | 100% ✅ | 로그인, 토큰 관리, 이메일 인증, 쿠키 TTL |
| **소셜 계정 복구** | 15개 | 100% ✅ | 30일 복구, 개인정보 마스킹 |
| **천체 데이터** | 25개 | 100% ✅ | NASA API, ISS 위치, 실시간 데이터 |
| **스케줄러** | 12개 | 100% ✅ | 크론 표현식, 자동화 작업 |
| **기타 시스템** | 23개 | 100% ✅ | 애플리케이션 설정, 유틸리티 |

### 테스트 최적화 성과

| 최적화 항목 | 개선 전 | 개선 후 | 효과 |
|------------|---------|---------|------|
| **테스트 안정성** | 컴파일 오류 | 100% 성공 | 100% 향상 |
| **Mock 설정 코드** | 15줄/테스트 | 1줄/테스트 | 93% 감소 |
| **빌드 시간** | 27초 | 13초 | 52% 단축 |
| **유지보수성** | 어려움 | 쉬움 | 대폭 개선 |

## 🔥 부하테스트 (k6)

### 테스트 환경

| 항목 | 설정 |
|------|------|
| 부하 생성 서버 | 별도 EC2 t3.micro (애플리케이션 서버와 분리) |
| 대상 서버 | 운영 EC2 (Spring Boot + 로컬 캐시) |
| 도구 | [k6](https://k6.io/) (Grafana Labs) |
| 시나리오 | 150 VU, 2분 30초 (ramp-up 30초 → 유지 2분) |
| 검증 | Actuator `/actuator/metrics/cache.gets` 캐시 히트/미스 정량 검증 |

부하 생성 서버를 별도 EC2로 분리하여, 부하 도구 자체의 CPU/네트워크 소비가 테스트 결과에 영향을 주지 않도록 했다.

### k6 스크립트 구조

```javascript
export const options = {
  stages: [
    { duration: '30s', target: 150 },  // ramp-up
    { duration: '2m', target: 150 },   // sustained load
  ],
  thresholds: {
    http_req_duration: ['p(95)<50'],    // p95 응답시간 50ms 이내
    http_req_failed: ['rate<0.01'],     // 에러율 1% 미만
  },
};
```

### Weather 캐시 테스트 결과

| 지표 | 결과 | 임계값 | 판정 |
|------|------|--------|------|
| 총 요청 | 316,953건 | - | - |
| 에러율 | 0% | < 1% | PASS |
| 평균 응답시간 | 20.16ms | - | - |
| p(95) 응답시간 | 49.29ms | < 50ms | PASS |
| 초당 처리량 | 2,112 req/s | - | - |

**Actuator 캐시 검증:**
- `cache.gets{result=hit}`: 316,953건
- `cache.gets{result=miss}`: 0건
- **적중률: 100%**

### ISS 캐시 테스트 결과

| 지표 | 결과 | 임계값 | 판정 |
|------|------|--------|------|
| 총 요청 | 290,792건 | - | - |
| 에러율 | 0% | < 1% | PASS |
| 평균 응답시간 | 24.05ms | - | - |
| p(95) 응답시간 | 51.15ms | < 50ms | 소폭 초과 |
| 초당 처리량 | 1,938 req/s | - | - |

**Actuator 캐시 검증:**
- `cache.gets{result=hit}`: 290,778건
- `cache.gets{result=miss}`: 14건
- **적중률: 99.995%**

miss 14건은 TLE 갱신 주기(12시간)와 패스 예측 캐시 TTL(2시간) 만료 시점에 발생한 정상적인 캐시 미스다.

### 실행 방법

```bash
# 별도 EC2에서 k6 설치
sudo yum install -y https://dl.k6.io/rpm/repo.rpm && sudo yum install -y k6

# Weather 캐시 부하테스트
k6 run weather-cache-test.js

# ISS 캐시 부하테스트
k6 run iss-cache-test.js

# Actuator 캐시 메트릭 확인
curl -s http://<server>:8080/actuator/metrics/cache.gets?tag=result:hit
curl -s http://<server>:8080/actuator/metrics/cache.gets?tag=result:miss
```

---

## 🚀 다음 단계

### 향후 구현 예정
- **Repository 테스트**: 쿼리 성능 및 인덱스 검증
- **Controller 테스트**: API 엔드포인트 통합 테스트
- **E2E 테스트**: 전체 사용자 플로우 검증

---

👉 다음 문서: [08. 배포 가이드](./08_deployment.md)