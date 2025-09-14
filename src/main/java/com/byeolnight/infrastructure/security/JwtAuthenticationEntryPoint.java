package com.byeolnight.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JWT 인증 실패 시 처리하는 엔트리 포인트
 * 인증되지 않은 요청에 대한 401 응답 및 로깅
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        
        String uri = request.getRequestURI();
        
        // 헬스체크 및 정적 리소스는 DEBUG 레벨로 로깅
        if (isLowPriorityPath(uri)) {
            log.debug("인증 불필요 요청: {} {}", request.getMethod(), uri);
        } else {
            log.warn("인증 실패: {} {} - {}", request.getMethod(), uri, authException.getMessage());
        }
        
        if (!response.isCommitted()) {
            SecurityUtils.writeAuthErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "인증이 필요합니다.");
        }
    }
    
    private boolean isLowPriorityPath(String uri) {
        return uri.contains("/health") || 
               uri.contains("/actuator") || 
               uri.contains("/favicon.ico") ||
               uri.contains("/robots.txt");
    }
}