package com.byeolnight.controller;

import com.byeolnight.infrastructure.monitoring.RequestLoggingFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {
    
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 요청 통계
        stats.put("totalRequests", RequestLoggingFilter.getTotalRequests());
        stats.put("httpRequests", RequestLoggingFilter.getHttpRequests());
        stats.put("wsRequests", RequestLoggingFilter.getWsRequests());
        stats.put("banStatusRequests", RequestLoggingFilter.getBanStatusRequests());
        
        // 메모리 사용량
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        
        Map<String, Object> memory = new HashMap<>();
        memory.put("used", heapUsage.getUsed() / 1024 / 1024); // MB
        memory.put("max", heapUsage.getMax() / 1024 / 1024); // MB
        memory.put("usagePercent", (double) heapUsage.getUsed() / heapUsage.getMax() * 100);
        
        stats.put("memory", memory);
        stats.put("timestamp", System.currentTimeMillis());
        
        return stats;
    }
}