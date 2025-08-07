package com.byeolnight.infrastructure.monitoring;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final AtomicLong requestCount = new AtomicLong(0);
    private static final AtomicLong banStatusRequestCount = new AtomicLong(0);
    private static final AtomicLong wsRequestCount = new AtomicLong(0);
    private static final AtomicLong httpRequestCount = new AtomicLong(0);
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String uri = request.getRequestURI();
        String method = request.getMethod();
        long count = requestCount.incrementAndGet();
        
        // 요청 타입별 분류
        if (uri.contains("/ws") || uri.contains("/sockjs")) {
            wsRequestCount.incrementAndGet();
        } else {
            httpRequestCount.incrementAndGet();
        }
        
        // 채팅 밴 상태 요청 카운트
        if (uri.contains("/chat/ban-status")) {
            long banCount = banStatusRequestCount.incrementAndGet();
            log.info("🚨 BAN_STATUS_REQUEST #{} - {} {}", banCount, method, uri);
        }
        
        // 매 50번째 요청마다 상세 통계 출력
        if (count % 50 == 0) {
            log.info("📊 총:{}, HTTP:{}, WS:{}, 밴상태:{}", 
                count, httpRequestCount.get(), wsRequestCount.get(), banStatusRequestCount.get());
        }
        
        filterChain.doFilter(request, response);
    }
    
    public static long getTotalRequests() {
        return requestCount.get();
    }
    
    public static long getBanStatusRequests() {
        return banStatusRequestCount.get();
    }
    
    public static long getWsRequests() {
        return wsRequestCount.get();
    }
    
    public static long getHttpRequests() {
        return httpRequestCount.get();
    }
}