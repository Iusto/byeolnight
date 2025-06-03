package com.byeolnight.infrastructure.security;

public class AuthWhitelist {
    public static final String[] PATHS = {
            "/api/auth/**",
            "/api/users/register",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/ws/chat/**"
    };
}
