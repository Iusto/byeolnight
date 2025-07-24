package com.byeolnight.infrastructure.security;

/**
 * 인증 없이 접근 가능한 URL 패턴 정의
 * Spring Security에서 permitAll() 처리할 경로들을 중앙 관리
 */
public class AuthWhitelist {
    public static final String[] PATHS = {
            "/api/public/**",
            "/api/auth/**",
            "/api/posts",           // 게시글 목록 조회
            "/api/posts/*",         // 게시글 단건 조회
            "/api/users/*/profile", // 사용자 프로필 조회
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/ws/**",
            "/api/suggestions",
            "/actuator/**",         // 헬스체크
            "/health",              // 헬스체크
            "/favicon.ico",         // 파비콘
            "/sitemap.xml",
            "/sitemap-*.xml",
            "/robots.txt",
            "/naver*.html"
    };
}
