package com.byeolnight.infrastructure.security;

public class AuthWhitelist {
    public static final String[] PATHS = {
            "/api/auth/**",
            "/api/auth/phone/**",
            "/api/auth/token/refresh",
            "/api/auth/check-nickname",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/ws/**",
            "/api/public/**"
    };
}
