package com.byeolnight.infrastructure.security;

/**
 * 인증 없이 접근 가능한 URL 패턴 정의
 * Spring Security에서 permitAll() 처리할 경로들을 중앙 관리
 */
public class AuthWhitelist {
    public static final String[] PATHS = {
            // 공개 API
            "/api/public/**",
            "/api/auth/**",
            "/oauth2/**",
            "/login/oauth2/**",
            "/api/posts",
            "/api/posts/**",        // 게시글 및 댓글 조회 포함
            "/api/users/*/profile", // 사용자 프로필 조회
            
            // API 문서 (개발환경)
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            
            // WebSocket
            "/ws/**",
            
            // 기타 공개 API (GET만 허용)
            "/api/suggestions",
            "/api/suggestions/*",
            
            // 헬스체크 (보안 강화)
            "/actuator/health",     // 헬스체크만 허용
            "/health",
            
            // 정적 리소스
            "/favicon.ico",
            "/sitemap.xml",
            "/sitemap-*.xml",
            "/robots.txt",
            "/naver-site-verification.html", // 구체적 파일명
            
            // 에러 페이지
            "/error"
    };
}
