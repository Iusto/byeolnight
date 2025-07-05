package com.byeolnight.infrastructure.security;

public class AuthWhitelist {
    public static final String[] PATHS = {
            "/api/public/**",
            "/api/auth/**",
            "/api/posts",           // 게시글 목록 조회
            "/api/posts/*",         // 게시글 단건 조회
            "/api/users/*/profile", // 사용자 프로필 조회
            "/api/admin/crawler/**", // 크롤러 API 허용
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/ws/**",
            "/api/comments/**",
            "/suggestions"
    };
}
