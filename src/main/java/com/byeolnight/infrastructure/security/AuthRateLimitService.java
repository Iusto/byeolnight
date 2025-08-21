package com.byeolnight.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 인증 요청 Rate Limiting 서비스
 * 이메일/SMS 인증 스팸 방지
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthRateLimitService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public boolean isEmailAuthAllowed(String email, String clientIp) {
        return checkEmailLimit(email) && checkIpAuthLimit(clientIp);
    }
    
    private boolean checkEmailLimit(String email) {
        return checkRateLimit("email_auth_1h:" + email, 5, 60, 60) &&
               checkRateLimit("email_auth_1d:" + email, 10, 1440, 1440);
    }
    
    private boolean checkIpAuthLimit(String clientIp) {
        // IP당 시간당 총 20개 인증 요청 (이메일+SMS 합계)
        if (!checkRateLimit("auth_total_1h:" + clientIp, 20, 60, 60)) {
            return false;
        }
        // IP당 일일 총 100개 인증 요청
        return checkRateLimit("auth_total_1d:" + clientIp, 100, 1440, 1440);
    }
    
    private boolean checkRateLimit(String key, int limit, int windowMinutes, int blockMinutes) {
        try {
            if (redisTemplate.hasKey(key + ":blocked")) return false;
            
            String countStr = redisTemplate.opsForValue().get(key);
            int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
            
            if (currentCount >= limit) {
                if (blockMinutes > 0) {
                    redisTemplate.opsForValue().set(key + ":blocked", "1", Duration.ofMinutes(blockMinutes));
                }
                return false;
            }
            
            if (currentCount == 0) {
                redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(windowMinutes));
            } else {
                redisTemplate.opsForValue().increment(key);
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }
    
    public void clearAuthLimit(String target) {
        String[] prefixes = {"email_auth_1h:", "email_auth_1d:", "auth_total_1h:", "auth_total_1d:"};
        
        for (String prefix : prefixes) {
            redisTemplate.delete(prefix + target);
            redisTemplate.delete(prefix + target + ":blocked");
        }
    }
}