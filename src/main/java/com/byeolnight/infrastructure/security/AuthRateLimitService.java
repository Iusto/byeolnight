package com.byeolnight.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 인증 요청 Rate Limiting 서비스
 * 이메일 인증 스팸 방지 및 IP 기반 제한
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthRateLimitService {
    
    private static final int EMAIL_HOURLY_LIMIT = 5;
    private static final int EMAIL_DAILY_LIMIT = 10;
    private static final int IP_HOURLY_LIMIT = 20;
    private static final int IP_DAILY_LIMIT = 100;
    private static final int HOUR_MINUTES = 60;
    private static final int DAY_MINUTES = 1440;
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public boolean isEmailAuthAllowed(String email, String clientIp) {
        return checkEmailLimit(email) && checkIpAuthLimit(clientIp);
    }
    
    private boolean checkEmailLimit(String email) {
        return checkRateLimit("email_auth_1h:" + email, EMAIL_HOURLY_LIMIT, HOUR_MINUTES, HOUR_MINUTES) &&
               checkRateLimit("email_auth_1d:" + email, EMAIL_DAILY_LIMIT, DAY_MINUTES, DAY_MINUTES);
    }
    
    private boolean checkIpAuthLimit(String clientIp) {
        return checkRateLimit("auth_total_1h:" + clientIp, IP_HOURLY_LIMIT, HOUR_MINUTES, HOUR_MINUTES) &&
               checkRateLimit("auth_total_1d:" + clientIp, IP_DAILY_LIMIT, DAY_MINUTES, DAY_MINUTES);
    }
    
    private boolean checkRateLimit(String key, int limit, int windowMinutes, int blockMinutes) {
        try {
            String blockedKey = key + ":blocked";
            if (Boolean.TRUE.equals(redisTemplate.hasKey(blockedKey))) {
                return false;
            }
            
            String countStr = redisTemplate.opsForValue().get(key);
            int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
            
            if (currentCount >= limit) {
                redisTemplate.opsForValue().set(blockedKey, "1", Duration.ofMinutes(blockMinutes));
                log.warn("Rate limit exceeded for key: {}, count: {}", key, currentCount);
                return false;
            }
            
            if (currentCount == 0) {
                redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(windowMinutes));
            } else {
                redisTemplate.opsForValue().increment(key);
            }
            return true;
        } catch (Exception e) {
            log.error("Rate limit check failed for key: {}", key, e);
            return true; // 실패 시 허용
        }
    }
    
    public void clearAuthLimit(String target) {
        String[] prefixes = {"email_auth_1h:", "email_auth_1d:", "auth_total_1h:", "auth_total_1d:"};
        
        for (String prefix : prefixes) {
            redisTemplate.delete(prefix + target);
            redisTemplate.delete(prefix + target + ":blocked");
        }
        log.info("Cleared auth limits for target: {}", target);
    }
}