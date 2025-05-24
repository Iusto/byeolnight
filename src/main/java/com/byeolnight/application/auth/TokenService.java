package com.byeolnight.application.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redisTemplate;

    public void delete(String refreshToken, String email) {
        redisTemplate.delete("refresh:" + email);
        System.out.println("[Logout] refreshToken for " + email + " removed");
    }
}
