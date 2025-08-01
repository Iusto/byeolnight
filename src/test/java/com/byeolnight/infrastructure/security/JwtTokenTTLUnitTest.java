package com.byeolnight.infrastructure.security;

import com.byeolnight.domain.entity.user.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JWT 토큰 TTL 단위 테스트")
class JwtTokenTTLUnitTest {

    private JwtTokenProvider jwtTokenProvider;
    private User testUser;

    @BeforeEach
    void setUp() {
        // JwtTokenProvider 직접 생성
        jwtTokenProvider = new JwtTokenProvider(null);
        
        // 테스트용 설정값 주입
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "testSecretKeyForJwtTokenTesting123456789");
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenValidity", Duration.ofMinutes(30));
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenValidity", Duration.ofDays(7));
        ReflectionTestUtils.setField(jwtTokenProvider, "allowedClockSkew", Duration.ofSeconds(30)); // 누락된 설정 추가
        
        // 테스트 사용자 생성
        testUser = User.builder()
                .email("test@example.com")
                .password("password")
                .nickname("테스트유저")
                .phone("01012345678")
                .role(User.Role.USER)
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);
    }

    @Test
    @DisplayName("Access Token TTL이 정확히 30분으로 설정되는지 검증")
    void accessToken_TTL_30분_검증() {
        // Given
        long beforeTokenCreation = System.currentTimeMillis();
        
        // When
        String accessToken = jwtTokenProvider.createAccessToken(testUser);
        
        // Then
        Claims claims = jwtTokenProvider.extractAllClaims(accessToken);
        Date expiration = claims.getExpiration();
        long tokenExpirationTime = expiration.getTime();
        
        // 30분 = 1,800,000 밀리초
        long expectedTTL = 30 * 60 * 1000L;
        long actualTTL = tokenExpirationTime - beforeTokenCreation;
        
        // 오차 범위 ±5초 허용
        assertThat(actualTTL)
                .describedAs("Access Token TTL이 30분(1,800,000ms)이어야 함")
                .isBetween(expectedTTL - 5000, expectedTTL + 5000);
        
        System.out.println("🔍 Access Token TTL 검증 결과:");
        System.out.println("   예상 TTL: " + expectedTTL + "ms (30분)");
        System.out.println("   실제 TTL: " + actualTTL + "ms");
        System.out.println("   차이: " + Math.abs(actualTTL - expectedTTL) + "ms");
        System.out.println("   만료 시간: " + expiration);
    }

    @Test
    @DisplayName("Refresh Token TTL이 정확히 7일로 설정되는지 검증")
    void refreshToken_TTL_7일_검증() {
        // Given
        long beforeTokenCreation = System.currentTimeMillis();
        
        // When
        String refreshToken = jwtTokenProvider.createRefreshToken(testUser);
        
        // Then
        Claims claims = jwtTokenProvider.extractAllClaims(refreshToken);
        Date expiration = claims.getExpiration();
        long tokenExpirationTime = expiration.getTime();
        
        // 7일 = 604,800,000 밀리초
        long expectedTTL = 7 * 24 * 60 * 60 * 1000L;
        long actualTTL = tokenExpirationTime - beforeTokenCreation;
        
        // 오차 범위 ±10초 허용
        assertThat(actualTTL)
                .describedAs("Refresh Token TTL이 7일(604,800,000ms)이어야 함")
                .isBetween(expectedTTL - 10000, expectedTTL + 10000);
        
        System.out.println("🔍 Refresh Token TTL 검증 결과:");
        System.out.println("   예상 TTL: " + expectedTTL + "ms (7일)");
        System.out.println("   실제 TTL: " + actualTTL + "ms");
        System.out.println("   차이: " + Math.abs(actualTTL - expectedTTL) + "ms");
        System.out.println("   만료 시간: " + expiration);
    }

    @Test
    @DisplayName("토큰 만료 시간 계산이 정확한지 검증")
    void 토큰_만료시간_계산_검증() {
        // Given & When
        String accessToken = jwtTokenProvider.createAccessToken(testUser);
        String refreshToken = jwtTokenProvider.createRefreshToken(testUser);
        
        // Then
        long accessTokenRemaining = jwtTokenProvider.getExpiration(accessToken);
        long refreshTokenRemaining = jwtTokenProvider.getExpiration(refreshToken);
        
        // Access Token: 30분 미만이어야 함
        assertThat(accessTokenRemaining)
                .describedAs("Access Token 남은 시간이 30분 미만이어야 함")
                .isLessThanOrEqualTo(30 * 60 * 1000L)
                .isGreaterThan(29 * 60 * 1000L);
        
        // Refresh Token: 7일 미만이어야 함
        assertThat(refreshTokenRemaining)
                .describedAs("Refresh Token 남은 시간이 7일 미만이어야 함")
                .isLessThanOrEqualTo(7 * 24 * 60 * 60 * 1000L)
                .isGreaterThan(6 * 24 * 60 * 60 * 1000L + 23 * 60 * 60 * 1000L);
        
        System.out.println("⏰ 토큰 만료 시간 검증:");
        System.out.println("   Access Token 남은 시간: " + Duration.ofMillis(accessTokenRemaining).toMinutes() + "분");
        System.out.println("   Refresh Token 남은 시간: " + Duration.ofMillis(refreshTokenRemaining).toDays() + "일");
    }

    @Test
    @DisplayName("토큰 유효성 검증 테스트")
    void 토큰_유효성_검증_테스트() {
        // Given
        String accessToken = jwtTokenProvider.createAccessToken(testUser);
        String refreshToken = jwtTokenProvider.createRefreshToken(testUser);
        
        // When & Then
        assertThat(jwtTokenProvider.validate(accessToken))
                .describedAs("새로 생성된 Access Token은 유효해야 함")
                .isTrue();
        
        assertThat(jwtTokenProvider.validateRefreshToken(refreshToken))
                .describedAs("새로 생성된 Refresh Token은 유효해야 함")
                .isTrue();
        
        // 이메일 추출 테스트
        String extractedEmail = jwtTokenProvider.getEmail(accessToken);
        assertThat(extractedEmail)
                .describedAs("토큰에서 추출한 이메일이 원본과 일치해야 함")
                .isEqualTo(testUser.getEmail());
        
        System.out.println("✅ 토큰 유효성 검증 완료");
        System.out.println("   추출된 이메일: " + extractedEmail);
    }

    @Test
    @DisplayName("application.yml 설정값 검증 (30분, 7일)")
    void 설정값_검증() {
        // Given
        long startTime = System.currentTimeMillis();
        
        // When
        String accessToken = jwtTokenProvider.createAccessToken(testUser);
        String refreshToken = jwtTokenProvider.createRefreshToken(testUser);
        
        // Then
        Claims accessClaims = jwtTokenProvider.extractAllClaims(accessToken);
        Claims refreshClaims = jwtTokenProvider.extractAllClaims(refreshToken);
        
        long accessTTL = accessClaims.getExpiration().getTime() - startTime;
        long refreshTTL = refreshClaims.getExpiration().getTime() - startTime;
        
        System.out.println("📋 application.yml 설정값 vs 실제값 비교:");
        System.out.println("   Access Token:");
        System.out.println("     - 설정: 30분 (1,800,000ms)");
        System.out.println("     - 실제: " + Duration.ofMillis(accessTTL).toMinutes() + "분 (" + accessTTL + "ms)");
        System.out.println("     - 정확도: " + String.format("%.2f%%", (double)accessTTL / 1800000 * 100));
        
        System.out.println("   Refresh Token:");
        System.out.println("     - 설정: 7일 (604,800,000ms)");
        System.out.println("     - 실제: " + Duration.ofMillis(refreshTTL).toDays() + "일 (" + refreshTTL + "ms)");
        System.out.println("     - 정확도: " + String.format("%.2f%%", (double)refreshTTL / 604800000 * 100));
        
        // 정확성 검증 (오차 ±1초)
        assertThat(accessTTL).isBetween(1800000L - 1000, 1800000L + 1000);
        assertThat(refreshTTL).isBetween(604800000L - 1000, 604800000L + 1000);
        
        System.out.println("✅ 설정값과 실제값이 정확히 일치합니다!");
    }
}