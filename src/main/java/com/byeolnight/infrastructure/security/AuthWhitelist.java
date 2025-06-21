package com.byeolnight.infrastructure.security;

public class AuthWhitelist {
    public static final String[] PATHS = {
            "/api/auth/login",
            "/api/auth/signup",
            "/api/auth/email/**",
            "/api/auth/phone/**",
            "/api/auth/token/refresh",
            "/api/auth/check-nickname",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/ws/chat/**",
            "/api/public/posts/**"
    };
}
