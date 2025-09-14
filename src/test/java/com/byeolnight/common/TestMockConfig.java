package com.byeolnight.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * 테스트용 공통 Mock 설정 유틸리티
 */
@TestConfiguration
@Profile("test")
public class TestMockConfig {

    @Bean
    @Primary
    @Profile("test")
    public RestTemplate testRestTemplate() {
        return mock(RestTemplate.class);
    }

    private static final String TEST_IP = "192.168.1.1";
    private static final String TEST_USER_AGENT = "Mozilla/5.0 Test Browser";

    /**
     * HttpServletRequest Mock 기본 설정
     * IpUtil.getClientIp()가 확인하는 모든 헤더 설정
     */
    public static void setupHttpServletRequest(HttpServletRequest request) {
        // IP 관련 헤더들
        given(request.getHeader("X-Client-IP")).willReturn(TEST_IP);
        given(request.getHeader("X-Forwarded-For")).willReturn(null);
        given(request.getHeader("X-Real-IP")).willReturn(null);
        given(request.getHeader("Proxy-Client-IP")).willReturn(null);
        given(request.getHeader("WL-Proxy-Client-IP")).willReturn(null);
        given(request.getHeader("HTTP_X_FORWARDED_FOR")).willReturn(null);
        given(request.getHeader("HTTP_X_FORWARDED")).willReturn(null);
        given(request.getHeader("HTTP_X_CLUSTER_CLIENT_IP")).willReturn(null);
        given(request.getHeader("HTTP_CLIENT_IP")).willReturn(null);
        given(request.getHeader("HTTP_FORWARDED_FOR")).willReturn(null);
        given(request.getHeader("HTTP_FORWARDED")).willReturn(null);
        given(request.getHeader("HTTP_VIA")).willReturn(null);
        given(request.getHeader("REMOTE_ADDR")).willReturn(null);
        
        // 기본 헤더들
        given(request.getHeader("User-Agent")).willReturn(TEST_USER_AGENT);
        given(request.getRemoteAddr()).willReturn(TEST_IP);
    }

    public static String getTestIp() {
        return TEST_IP;
    }

    public static String getTestUserAgent() {
        return TEST_USER_AGENT;
    }

    /**
     * 테스트용 Mock 초기화 메서드
     */
    public static void resetAllMocks(Object... mocks) {
        for (Object mock : mocks) {
            if (mock != null) {
                org.mockito.Mockito.reset(mock);
            }
        }
    }
}