package com.byeolnight.infrastructure.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 * HTTP 요청 본문을 캐싱하여 여러 번 읽을 수 있도록 하는 필터
 * 인앱브라우저에서 발생하는 "getInputStream() has already been called" 오류를 방지
 */
@Component
public class ContentCachingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            
            // POST 요청이고 Content-Type이 application/json인 경우에만 래핑
            if ("POST".equalsIgnoreCase(httpRequest.getMethod()) && 
                httpRequest.getContentType() != null && 
                httpRequest.getContentType().contains("application/json")) {
                
                ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
                chain.doFilter(wrappedRequest, response);
            } else {
                chain.doFilter(request, response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}