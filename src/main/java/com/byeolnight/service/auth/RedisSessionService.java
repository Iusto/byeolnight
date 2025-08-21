package com.byeolnight.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSessionService {

    private final StringRedisTemplate redisTemplate;
    
    private static final String ROTATE_TOKEN_SCRIPT = """
        local sessionKey = KEYS[1]
        local newRefreshToken = ARGV[1]
        local expiry = ARGV[2]
        
        if redis.call('EXISTS', sessionKey) == 1 then
            redis.call('SET', sessionKey, newRefreshToken, 'EX', expiry)
            return 1
        else
            return 0
        end
        """;

    public boolean isValidSession(String sessionId) {
        String sessionKey = "session:" + sessionId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey));
    }

    public boolean rotateRefreshToken(String sessionId, String newRefreshToken) {
        String sessionKey = "session:" + sessionId;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(ROTATE_TOKEN_SCRIPT, Long.class);
        
        Long result = redisTemplate.execute(script, 
            Collections.singletonList(sessionKey), 
            newRefreshToken, 
            String.valueOf(Duration.ofDays(7).getSeconds())
        );
        
        return result != null && result == 1L;
    }

    public void invalidateSession(String sessionId) {
        String sessionKey = "session:" + sessionId;
        redisTemplate.delete(sessionKey);
    }
}