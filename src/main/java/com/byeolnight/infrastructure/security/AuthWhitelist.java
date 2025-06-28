package com.byeolnight.infrastructure.security;

public class AuthWhitelist {
    public static final String[] PATHS = {
            "/api/public/**",
            "/api/auth/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/ws/**",
            "api/comments/**"
    };
}
