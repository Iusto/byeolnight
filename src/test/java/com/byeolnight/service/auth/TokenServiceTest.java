package com.byeolnight.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * TokenService 테스트 - Redis 기반 토큰 관리 검증
 * 
 * 테스트 범위:
 * 1. Refresh Token 저장/조회/삭제
 * 2. Access Token 블랙리스트 관리
 * 3. 토큰 해싱 및 키 생성
 * 4. TTL 기반 만료 처리
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService 테스트")
class TokenServiceTest {

    @InjectMocks
    private TokenService tokenService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_REFRESH_TOKEN = "refresh.token.example";
    private final String TEST_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
    private final long TEST_EXPIRATION = 604800000L; // 7일

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Nested
    @DisplayName("Refresh Token 관리")
    class RefreshTokenManagement {

        @Test
        @DisplayName("Refresh Token 저장 - Redis에 올바른 키와 TTL로 저장")
        void saveRefreshToken_StoresWithCorrectKeyAndTTL() {
            // When
            tokenService.saveRefreshToken(TEST_EMAIL, TEST_REFRESH_TOKEN, TEST_EXPIRATION);

            // Then
            verify(valueOperations).set(
                eq("refresh:" + TEST_EMAIL),
                eq(TEST_REFRESH_TOKEN),
                eq(TEST_EXPIRATION),
                eq(TimeUnit.MILLISECONDS)
            );
        }

        @Test
        @DisplayName("유효한 Refresh Token 검증 - 저장된 토큰과 일치")
        void isValidRefreshToken_ValidToken_ReturnsTrue() {
            // Given
            given(valueOperations.get("refresh:" + TEST_EMAIL)).willReturn(TEST_REFRESH_TOKEN);

            // When
            boolean isValid = tokenService.isValidRefreshToken(TEST_EMAIL, TEST_REFRESH_TOKEN);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("유효하지 않은 Refresh Token 검증 - 다른 토큰")
        void isValidRefreshToken_InvalidToken_ReturnsFalse() {
            // Given
            given(valueOperations.get("refresh:" + TEST_EMAIL)).willReturn(TEST_REFRESH_TOKEN);

            // When
            boolean isValid = tokenService.isValidRefreshToken(TEST_EMAIL, "different.token");

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 Refresh Token 검증 - null 반환")
        void isValidRefreshToken_NonExistentToken_ReturnsFalse() {
            // Given
            given(valueOperations.get("refresh:" + TEST_EMAIL)).willReturn(null);

            // When
            boolean isValid = tokenService.isValidRefreshToken(TEST_EMAIL, TEST_REFRESH_TOKEN);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Refresh Token 삭제 - delete 메서드")
        void delete_RemovesRefreshToken() {
            // When
            tokenService.delete(TEST_REFRESH_TOKEN, TEST_EMAIL);

            // Then
            verify(redisTemplate).delete("refresh:" + TEST_EMAIL);
        }

        @Test
        @DisplayName("Refresh Token 삭제 - deleteRefreshToken 메서드")
        void deleteRefreshToken_RemovesRefreshToken() {
            // When
            tokenService.deleteRefreshToken(TEST_EMAIL);

            // Then
            verify(redisTemplate).delete("refresh:" + TEST_EMAIL);
        }
    }

    @Nested
    @DisplayName("Access Token 블랙리스트 관리")
    class AccessTokenBlacklistManagement {

        @Test
        @DisplayName("Access Token 블랙리스트 등록 - 해싱된 키로 저장")
        void blacklistAccessToken_StoresWithHashedKey() {
            // When
            tokenService.blacklistAccessToken(TEST_ACCESS_TOKEN, TEST_EXPIRATION);

            // Then
            verify(valueOperations).set(
                startsWith("blacklist:"),
                eq("true"),
                eq(TEST_EXPIRATION),
                eq(TimeUnit.MILLISECONDS)
            );
        }

        @Test
        @DisplayName("블랙리스트된 Access Token 확인 - true 반환")
        void isAccessTokenBlacklisted_BlacklistedToken_ReturnsTrue() {
            // Given
            given(redisTemplate.hasKey(startsWith("blacklist:"))).willReturn(true);

            // When
            boolean isBlacklisted = tokenService.isAccessTokenBlacklisted(TEST_ACCESS_TOKEN);

            // Then
            assertThat(isBlacklisted).isTrue();
        }

        @Test
        @DisplayName("블랙리스트되지 않은 Access Token 확인 - false 반환")
        void isAccessTokenBlacklisted_NonBlacklistedToken_ReturnsFalse() {
            // Given
            given(redisTemplate.hasKey(startsWith("blacklist:"))).willReturn(false);

            // When
            boolean isBlacklisted = tokenService.isAccessTokenBlacklisted(TEST_ACCESS_TOKEN);

            // Then
            assertThat(isBlacklisted).isFalse();
        }

        @Test
        @DisplayName("블랙리스트 키 존재하지 않음 - false 반환")
        void isAccessTokenBlacklisted_KeyNotExists_ReturnsFalse() {
            // Given
            given(redisTemplate.hasKey(startsWith("blacklist:"))).willReturn(null);

            // When
            boolean isBlacklisted = tokenService.isAccessTokenBlacklisted(TEST_ACCESS_TOKEN);

            // Then
            assertThat(isBlacklisted).isFalse();
        }
    }

    @Nested
    @DisplayName("토큰 해싱 및 키 생성")
    class TokenHashingAndKeyGeneration {

        @Test
        @DisplayName("동일한 토큰은 동일한 블랙리스트 키 생성")
        void blacklistAccessToken_SameToken_GeneratesSameKey() {
            // Given
            String sameToken = TEST_ACCESS_TOKEN;

            // When
            tokenService.blacklistAccessToken(TEST_ACCESS_TOKEN, TEST_EXPIRATION);
            tokenService.blacklistAccessToken(sameToken, TEST_EXPIRATION);

            // Then - 동일한 키로 두 번 호출되어야 함
            verify(valueOperations, times(2)).set(
                startsWith("blacklist:"),
                eq("true"),
                eq(TEST_EXPIRATION),
                eq(TimeUnit.MILLISECONDS)
            );
        }

        @Test
        @DisplayName("다른 토큰은 다른 블랙리스트 키 생성")
        void blacklistAccessToken_DifferentTokens_GeneratesDifferentKeys() {
            // Given
            String anotherToken = "different.jwt.token.here";

            // When
            tokenService.blacklistAccessToken(TEST_ACCESS_TOKEN, TEST_EXPIRATION);
            tokenService.blacklistAccessToken(anotherToken, TEST_EXPIRATION);

            // Then
            verify(valueOperations, times(2)).set(
                startsWith("blacklist:"),
                eq("true"),
                eq(TEST_EXPIRATION),
                eq(TimeUnit.MILLISECONDS)
            );
        }
    }

    @Nested
    @DisplayName("TTL 및 만료 처리")
    class TTLAndExpirationHandling {

        @Test
        @DisplayName("짧은 TTL로 블랙리스트 등록")
        void blacklistAccessToken_ShortTTL_StoresCorrectly() {
            // Given
            long shortTTL = 1800000L; // 30분

            // When
            tokenService.blacklistAccessToken(TEST_ACCESS_TOKEN, shortTTL);

            // Then
            verify(valueOperations).set(
                startsWith("blacklist:"),
                eq("true"),
                eq(shortTTL),
                eq(TimeUnit.MILLISECONDS)
            );
        }

        @Test
        @DisplayName("긴 TTL로 Refresh Token 저장")
        void saveRefreshToken_LongTTL_StoresCorrectly() {
            // Given
            long longTTL = 2592000000L; // 30일

            // When
            tokenService.saveRefreshToken(TEST_EMAIL, TEST_REFRESH_TOKEN, longTTL);

            // Then
            verify(valueOperations).set(
                eq("refresh:" + TEST_EMAIL),
                eq(TEST_REFRESH_TOKEN),
                eq(longTTL),
                eq(TimeUnit.MILLISECONDS)
            );
        }
    }

    @Nested
    @DisplayName("엣지 케이스 처리")
    class EdgeCaseHandling {

        @Test
        @DisplayName("빈 문자열 이메일 처리")
        void saveRefreshToken_EmptyEmail_HandlesCorrectly() {
            // Given
            String emptyEmail = "";

            // When
            tokenService.saveRefreshToken(emptyEmail, TEST_REFRESH_TOKEN, TEST_EXPIRATION);

            // Then
            verify(valueOperations).set(
                eq("refresh:"),
                eq(TEST_REFRESH_TOKEN),
                eq(TEST_EXPIRATION),
                eq(TimeUnit.MILLISECONDS)
            );
        }

        @Test
        @DisplayName("매우 긴 JWT 토큰 블랙리스트 처리")
        void blacklistAccessToken_VeryLongToken_HandlesCorrectly() {
            // Given
            String veryLongToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ." +
                    "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c".repeat(10);

            // When
            tokenService.blacklistAccessToken(veryLongToken, TEST_EXPIRATION);

            // Then
            verify(valueOperations).set(
                startsWith("blacklist:"),
                eq("true"),
                eq(TEST_EXPIRATION),
                eq(TimeUnit.MILLISECONDS)
            );
        }

        @Test
        @DisplayName("0 TTL 처리")
        void saveRefreshToken_ZeroTTL_HandlesCorrectly() {
            // Given
            long zeroTTL = 0L;

            // When
            tokenService.saveRefreshToken(TEST_EMAIL, TEST_REFRESH_TOKEN, zeroTTL);

            // Then
            verify(valueOperations).set(
                eq("refresh:" + TEST_EMAIL),
                eq(TEST_REFRESH_TOKEN),
                eq(zeroTTL),
                eq(TimeUnit.MILLISECONDS)
            );
        }
    }
}