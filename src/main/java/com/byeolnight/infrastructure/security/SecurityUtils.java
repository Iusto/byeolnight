package com.byeolnight.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;

/**
 * 보안 관련 공통 유틸리티
 * JWT 토큰 추출 및 에러 응답 생성
 */
public final class SecurityUtils {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String ACCESS_TOKEN_COOKIE = "accessToken";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";
    
    private SecurityUtils() {} // 유틸리티 클래스
    
    /**
     * HTTP 요청에서 JWT 토큰 추출
     * 1순위: HttpOnly 쿠키, 2순위: Authorization 헤더
     */
    public static String resolveToken(HttpServletRequest request) {
        // 1. HttpOnly 쿠키에서 토큰 추출 (보안 우선)
        String tokenFromCookie = extractTokenFromCookie(request);
        if (tokenFromCookie != null) {
            return tokenFromCookie;
        }
        
        // 2. Authorization 헤더에서 추출 (후방 호환성)
        return extractTokenFromHeader(request);
    }
    
    private static String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_TOKEN_COOKIE.equals(cookie.getName()) && 
                    StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    
    private static String extractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
    
    /**
     * 인증 실패 JSON 응답 생성
     */
    public static void writeAuthErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(JSON_CONTENT_TYPE);
        
        Map<String, Object> errorResponse = Map.of(
            "success", false,
            "message", message,
            "code", status == HttpServletResponse.SC_UNAUTHORIZED ? "UNAUTHORIZED" : "FORBIDDEN"
        );
        
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(errorResponse));
    }
}