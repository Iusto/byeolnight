package com.byeolnight.infrastructure.security;

import com.byeolnight.entity.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("복잡한 통합 테스트는 임시 비활성화")
class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("JWT Access Token 30분 수명 설정 검증")
    void testAccessTokenExpiration() {
        // Given: 테스트용 사용자 생성
        User testUser = User.builder()
                .email("test@example.com")
                .password("password")
                .nickname("testuser")
                .phone("01012345678")
                .phoneHash("hash123")
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        // When: Access Token 생성
        String accessToken = jwtTokenProvider.createAccessToken(testUser);
        
        // Then: 토큰 유효성 검증
        assertThat(accessToken).isNotNull();
        assertThat(jwtTokenProvider.validate(accessToken)).isTrue();
        
        // 토큰 만료 시간 검증 (30분 = 1800초 = 1,800,000ms)
        long expiration = jwtTokenProvider.getExpiration(accessToken);
        
        // 약간의 오차를 고려하여 29분 30초 ~ 30분 사이인지 확인
        long thirtyMinutesInMs = 30 * 60 * 1000; // 30분
        long twentyNineMinutesThirtySecondsInMs = 29 * 60 * 1000 + 30 * 1000; // 29분 30초
        
        assertThat(expiration)
                .isGreaterThan(twentyNineMinutesThirtySecondsInMs)
                .isLessThanOrEqualTo(thirtyMinutesInMs);
        
        System.out.println("✅ Access Token 만료 시간: " + (expiration / 1000 / 60) + "분 " + 
                          ((expiration / 1000) % 60) + "초");
    }

    @Test
    @DisplayName("JWT Refresh Token 7일 수명 설정 검증")
    void testRefreshTokenExpiration() {
        // Given: 테스트용 사용자 생성
        User testUser = User.builder()
                .email("test@example.com")
                .password("password")
                .nickname("testuser")
                .phone("01012345678")
                .phoneHash("hash123")
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        // When: Refresh Token 생성
        String refreshToken = jwtTokenProvider.createRefreshToken(testUser);
        
        // Then: 토큰 유효성 검증
        assertThat(refreshToken).isNotNull();
        assertThat(jwtTokenProvider.validateRefreshToken(refreshToken)).isTrue();
        
        // 토큰 만료 시간 검증 (7일 = 604,800초 = 604,800,000ms)
        long expiration = jwtTokenProvider.getExpiration(refreshToken);
        
        // 약간의 오차를 고려하여 6일 23시간 ~ 7일 사이인지 확인
        long sevenDaysInMs = 7 * 24 * 60 * 60 * 1000L; // 7일
        long sixDaysTwentyThreeHoursInMs = (6 * 24 + 23) * 60 * 60 * 1000L; // 6일 23시간
        
        assertThat(expiration)
                .isGreaterThan(sixDaysTwentyThreeHoursInMs)
                .isLessThanOrEqualTo(sevenDaysInMs);
        
        System.out.println("✅ Refresh Token 만료 시간: " + (expiration / 1000 / 60 / 60 / 24) + "일 " + 
                          ((expiration / 1000 / 60 / 60) % 24) + "시간");
    }
}