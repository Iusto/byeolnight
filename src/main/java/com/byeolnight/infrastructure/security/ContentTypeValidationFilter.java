package com.byeolnight.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Content-Type 검증 필터
 * 잘못된 Content-Type 요청 차단 및 보안 강화
 */
@Slf4j
public class ContentTypeValidationFilter extends OncePerRequestFilter {

    private static final Set<String> BODY_METHODS = Set.of("POST", "PUT", "PATCH");
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String JAVASCRIPT_CONTENT_TYPE = "application/javascript";
    private static final String UTF8_ENCODING = "UTF-8";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String contentType = request.getContentType();
        String method = request.getMethod();
        
        if (BODY_METHODS.contains(method) && contentType != null) {
            if (isInvalidContentType(contentType)) {
                writeErrorResponse(response, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    "지원하지 않는 콘텐츠 타입입니다. application/json을 사용해주세요.");
                log.warn("잘못된 Content-Type 차단: {} from {}", contentType, request.getRemoteAddr());
                return;
            }
            
            if (isInvalidEncoding(contentType, request.getCharacterEncoding())) {
                writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "잘못된 문자 인코딩입니다. UTF-8을 사용해주세요.");
                log.warn("잘못된 인코딩: {} from {}", request.getCharacterEncoding(), request.getRemoteAddr());
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean isInvalidContentType(String contentType) {
        return contentType.contains(JAVASCRIPT_CONTENT_TYPE);
    }
    
    private boolean isInvalidEncoding(String contentType, String encoding) {
        return contentType.startsWith(JSON_CONTENT_TYPE) && 
               encoding != null && 
               !UTF8_ENCODING.equalsIgnoreCase(encoding);
    }
    
    private void writeErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
            String.format("{\"success\":false,\"message\":\"%s\"}", message)
        );
    }
}