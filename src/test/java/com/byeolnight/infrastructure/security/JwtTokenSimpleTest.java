package com.byeolnight.infrastructure.security;

import com.byeolnight.entity.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JWT 토큰 간단 테스트")
class JwtTokenSimpleTest {

    private JwtTokenProvider jwtTokenProvider;
    private User testUser;

    @BeforeEach
    void setUp() {
        // JwtTokenProvider 직접 생성
        jwtTokenProvider = new JwtTokenProvider(null, null);
        
        // 테스트용 설정값 주입
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "testSecretKeyForJwtTokenTesting123456789012345678901234567890");
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenValidity", Duration.ofMinutes(30));
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenValidity", Duration.ofDays(7));
        ReflectionTestUtils.setField(jwtTokenProvider, "allowedClockSkew", Duration.ofSeconds(30));
        
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
    @DisplayName("Access Token 생성 및 검증")
    void accessToken_생성_및_검증() {
        // When
        String accessToken = jwtTokenProvider.createAccessToken(testUser);
        
        // Then
        assertThat(accessToken).isNotNull();
        assertThat(jwtTokenProvider.validate(accessToken)).isTrue();
        
        String email = jwtTokenProvider.getEmail(accessToken);
        assertThat(email).isEqualTo("test@example.com");
        
        System.out.println("✅ Access Token 생성 및 검증 성공");
    }

    @Test
    @DisplayName("Refresh Token 생성 및 검증")
    void refreshToken_생성_및_검증() {
        // When
        String refreshToken = jwtTokenProvider.createRefreshToken(testUser);
        
        // Then
        assertThat(refreshToken).isNotNull();
        assertThat(jwtTokenProvider.validateRefreshToken(refreshToken)).isTrue();
        
        System.out.println("✅ Refresh Token 생성 및 검증 성공");
    }

    @Test
    @DisplayName("토큰 만료 시간 확인")
    void 토큰_만료시간_확인() {
        // When
        String accessToken = jwtTokenProvider.createAccessToken(testUser);
        String refreshToken = jwtTokenProvider.createRefreshToken(testUser);
        
        // Then
        long accessExpiration = jwtTokenProvider.getExpiration(accessToken);
        long refreshExpiration = jwtTokenProvider.getExpiration(refreshToken);
        
        // Access Token은 30분 미만
        assertThat(accessExpiration).isLessThanOrEqualTo(30 * 60 * 1000L);
        assertThat(accessExpiration).isGreaterThan(29 * 60 * 1000L);
        
        // Refresh Token은 7일 미만
        assertThat(refreshExpiration).isLessThanOrEqualTo(7 * 24 * 60 * 60 * 1000L);
        assertThat(refreshExpiration).isGreaterThan(6 * 24 * 60 * 60 * 1000L);
        
        System.out.println("✅ 토큰 만료 시간 확인 성공");
        System.out.println("   Access Token: " + (accessExpiration / 1000 / 60) + "분");
        System.out.println("   Refresh Token: " + (refreshExpiration / 1000 / 60 / 60 / 24) + "일");
    }
}