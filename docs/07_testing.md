# 07. 테스트 전략

> 실제 운영에서 발생 가능한 시나리오를 중심으로 테스트 설계

## 🧪 테스트 계층 구성

| 계층              | 전략                  | 도구/어노테이션                              |
| --------------- | ------------------- | ------------------------------------- |
| **Controller**  | 인증/권한/예외 흐름 검증      | `@WebMvcTest`, MockMvc                |
| **Service**     | 유즈케이스별 로직 테스트       | `@ExtendWith(MockitoExtension.class)` |
| **Repository**  | 쿼리 정확도 및 연관관계 테스트   | `@DataJpaTest`                        |
| **Integration** | end-to-end 전체 흐름 검증 | `@SpringBootTest`, 테스트 컨테이너           |

## ✅ 주요 테스트 시나리오 예시

### 📌 로그인 실패 시 계정 잠금

```java
@Test
void 로그인_실패_횟수_증가_테스트() {
    // Given
    User user = createTestUser();
    String wrongPassword = "wrongPassword";

    // When
    for (int i = 0; i < 5; i++) {
        userService.increaseLoginFailCount(user, "127.0.0.1", "TestAgent");
    }

    // Then
    assertTrue(user.isAccountLocked());
    assertEquals(5, user.getLoginFailCount());
}
```

### 📌 JWT TTL 검증

```java
@Test
@DisplayName("Access Token TTL이 정확히 30분으로 설정되는지 검증")
void accessToken_TTL_검증() {
    String accessToken = jwtTokenProvider.createAccessToken(testUser);
    Claims claims = jwtTokenProvider.extractAllClaims(accessToken);
    long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
    assertThat(ttl).isBetween(1795000L, 1805000L); // ±5초 허용
}
```

## 🔄 자동화 및 커버리지

* GitHub Actions를 통한 push → 테스트 자동 실행
* 향후 TestContainers 도입 검토 (Redis, MySQL 포함 환경)
* 코드 커버리지 기준 70% 이상 유지 중

---

👉 다음 문서: [08. 운영 및 배포 전략](./08_deployment.md)
