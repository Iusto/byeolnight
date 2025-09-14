package com.byeolnight.infrastructure.security;

/**
 * 인증 없이 접근 가능한 URL 패턴 정의
 * Spring Security permitAll() 경로 중앙 관리
 */
public final class AuthWhitelist {
    
    private AuthWhitelist() {} // 유틸리티 클래스
    
    public static final String[] PATHS = {
            // 인증 관련
            "/api/auth/**",
            "/oauth2/**",
            "/login/oauth2/**",
            
            // 공개 API
            "/api/public/**",
            "/public/**",
            "/api/posts",
            "/api/posts/*/comments",
            "/api/users/*/profile",
            "/api/weather/**",
            
            // WebSocket
            "/ws/**",
            
            // API 문서
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            
            // 헬스체크
            "/actuator/health",
            "/health",
            
            // 정적 리소스
            "/favicon.ico",
            "/robots.txt",
            "/sitemap*.xml",
            "/naver*.html",
            
            // 에러 페이지
            "/error"
    };
}
