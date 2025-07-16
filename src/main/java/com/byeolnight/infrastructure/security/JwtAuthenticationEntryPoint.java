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
 *
 * 역할:
 * - 인증되지 않은 요청에 대해 401 Unauthorized 응답
 * - 일관된 JSON 에러 메시지 반환
 * - 인증 실패 로그 기록
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        
        log.warn("인증되지 않은 요청: {} {}", request.getMethod(), request.getRequestURI());
        
        SecurityUtils.writeAuthErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "인증이 필요합니다.");
    }
}