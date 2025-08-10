package com.byeolnight.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 보안 관련 공통 유틸리티
 * 
 * 역할:
 * - HTTP 요청에서 JWT 토큰 추출
 * - 일관된 JSON 에러 응답 생성 (401/403)
 * - 보안 관련 공통 로직 중앙화
 */
public class SecurityUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * HTTP 요청에서 JWT 토큰 추출
     * 1. 쿠키에서 토큰 추출 시도
     * 2. 쿠키에 없으면 Authorization 헤더에서 추출 시도
     */
    public static String resolveToken(HttpServletRequest request) {
        // 1. 쿠키에서 토큰 추출 시도
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        // 2. Authorization 헤더에서 추출 시도 (후방 호환성)
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            System.out.println("Authorization 헤더에서 토큰 추출 성공: " + token.substring(0, Math.min(10, token.length())) + "...");
            return token;
        }
        
        return null;
    }
    
    /**
     * 인증 실패 JSON 응답 생성
     */
    public static void writeAuthErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("code", status == 401 ? "UNAUTHORIZED" : "FORBIDDEN");
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}