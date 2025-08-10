package com.byeolnight.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Content-Type 검증 필터
 * 잘못된 Content-Type 요청을 사전에 차단
 */
@Slf4j
public class ContentTypeValidationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String contentType = request.getContentType();
        String method = request.getMethod();
        
        // POST, PUT, PATCH 요청에서 Content-Type 검증
        if (("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) 
            && contentType != null) {
            
            // application/javascript 요청 차단
            if (contentType.contains("application/javascript")) {
                log.warn("잘못된 Content-Type 요청 차단: {} from {}", 
                        contentType, request.getRemoteAddr());
                
                response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                    "{\"success\":false,\"message\":\"지원하지 않는 콘텐츠 타입입니다. application/json을 사용해주세요.\"}"
                );
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}