package com.byeolnight.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

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

    private static final String CHECK_RATE_LIMIT_SCRIPT = """
        local key = KEYS[1]
        local blockedKey = KEYS[2]
        local limit = tonumber(ARGV[1])
        local windowSeconds = tonumber(ARGV[2])
        local blockSeconds = tonumber(ARGV[3])

        if redis.call('EXISTS', blockedKey) == 1 then
            return -1
        end

        local current = tonumber(redis.call('GET', key) or '0')

        if current >= limit then
            if blockSeconds > 0 then
                redis.call('SET', blockedKey, '1', 'EX', blockSeconds)
            end
            return -1
        end

        if current == 0 then
            redis.call('SET', key, '1', 'EX', windowSeconds)
        else
            redis.call('INCR', key)
        end

        return current + 1
        """;
    
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
            List<String> keys = Arrays.asList(key, blockedKey);

            DefaultRedisScript<Long> script = new DefaultRedisScript<>(CHECK_RATE_LIMIT_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, keys,
                    String.valueOf(limit),
                    String.valueOf(windowMinutes * 60L),
                    String.valueOf(blockMinutes * 60L));

            if (result != null && result == -1L) {
                log.warn("Rate limit exceeded for key: {}", key);
                return false;
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