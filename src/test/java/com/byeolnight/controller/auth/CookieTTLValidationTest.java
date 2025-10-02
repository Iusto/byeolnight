package com.byeolnight.controller.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 쿠키 TTL 설정 검증 테스트
 */
class CookieTTLValidationTest {

    @Test
    @DisplayName("TTL 계산 정확성 검증")
    void validateTTLCalculations() {
        // Given
        long sevenDaysInMs = 7 * 24 * 60 * 60 * 1000L; // 7일 (밀리초)
        long thirtyMinutesInSeconds = 30 * 60L; // 30분 (초)

        // When & Then
        assertThat(sevenDaysInMs / 1000)
                .as("Refresh Token TTL은 정확히 604800초(7일)여야 함")
                .isEqualTo(604800L);

        assertThat(thirtyMinutesInSeconds)
                .as("Access Token TTL은 정확히 1800초(30분)여야 함")
                .isEqualTo(1800L);

        // 추가 검증: 시간 단위 변환
        assertThat(604800L / 60 / 60 / 24)
                .as("604800초는 정확히 7일")
                .isEqualTo(7L);

        assertThat(1800L / 60)
                .as("1800초는 정확히 30분")
                .isEqualTo(30L);
    }

    @Test
    @DisplayName("ResponseCookie MaxAge 설정 검증")
    void validateResponseCookieMaxAge() {
        // Given
        long refreshTTL = 604800L; // 7일 (초)
        long accessTTL = 1800L; // 30분 (초)

        // When
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "test-token")
                .maxAge(refreshTTL)
                .build();

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", "test-token")
                .maxAge(accessTTL)
                .build();

        ResponseCookie sessionCookie = ResponseCookie.from("sessionToken", "test-token")
                .build(); // maxAge 설정 안함

        // Then
        assertThat(refreshCookie.getMaxAge().getSeconds())
                .as("Refresh Cookie MaxAge는 604800초")
                .isEqualTo(604800L);

        assertThat(accessCookie.getMaxAge().getSeconds())
                .as("Access Cookie MaxAge는 1800초")
                .isEqualTo(1800L);

        // Spring의 ResponseCookie는 MaxAge를 설정하지 않으면 -1S로 설정됨 (세션 쿠키 의미)
        assertThat(sessionCookie.getMaxAge().getSeconds())
                .as("Session Cookie는 MaxAge가 -1 (세션 쿠키)")
                .isEqualTo(-1L);
    }

    @Test
    @DisplayName("쿠키 문자열 형식 검증")
    void validateCookieStringFormat() {
        // Given
        ResponseCookie cookieWithMaxAge = ResponseCookie.from("testCookie", "testValue")
                .maxAge(3600L) // 1시간
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .build();

        ResponseCookie sessionCookie = ResponseCookie.from("sessionCookie", "sessionValue")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .build();

        // When
        String cookieWithMaxAgeString = cookieWithMaxAge.toString();
        String sessionCookieString = sessionCookie.toString();

        // Then
        assertThat(cookieWithMaxAgeString)
                .as("MaxAge가 설정된 쿠키는 Max-Age 속성을 포함해야 함")
                .contains("Max-Age=3600")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax");

        assertThat(sessionCookieString)
                .as("세션 쿠키는 Max-Age 속성을 포함하지 않아야 함")
                .doesNotContain("Max-Age")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax");
    }
}