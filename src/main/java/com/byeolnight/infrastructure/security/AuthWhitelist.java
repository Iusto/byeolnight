package com.byeolnight.infrastructure.security;

/**
 * 인증 없이 접근 가능한 URL 패턴 정의
 * Spring Security에서 permitAll() 처리할 경로들을 중앙 관리
 */
public class AuthWhitelist {
    public static final String[] PATHS = {
            // 인증 관련
            "/api/auth/**",
            "/oauth2/**",
            "/login/oauth2/**",
            
            // 공개 API (읽기 전용)
            "/api/public/**",
            "/public/**",
            "/api/posts",           // 게시글 목록 조회
            "/api/posts/*/comments", // 댓글 조회
            "/api/users/*/profile", // 프로필 조회
            
            // 로컬 개발용
            "/member/users/me",
            
            // API 문서
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            
            // WebSocket
            "/ws/**",
            
            // 공개 데이터 (GET만 허용)
            // 건의사항은 컨트롤러에서 개별 처리
            
            // 날씨 및 천체 정보 (공개 접근)
            "/api/weather/**",
            
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
