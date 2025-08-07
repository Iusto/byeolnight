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
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String uri = request.getRequestURI();
        String method = request.getMethod();
        long count = requestCount.incrementAndGet();
        
        // 채팅 밴 상태 요청 카운트
        if (uri.contains("/chat/ban-status")) {
            long banCount = banStatusRequestCount.incrementAndGet();
            log.info("🚨 BAN_STATUS_REQUEST #{} - {} {}", banCount, method, uri);
        }
        
        // 매 100번째 요청마다 전체 통계 출력
        if (count % 100 == 0) {
            log.info("📊 총 요청 수: {}, 밴상태 요청: {}", count, banStatusRequestCount.get());
        }
        
        filterChain.doFilter(request, response);
    }
    
    public static long getTotalRequests() {
        return requestCount.get();
    }
    
    public static long getBanStatusRequests() {
        return banStatusRequestCount.get();
    }
}