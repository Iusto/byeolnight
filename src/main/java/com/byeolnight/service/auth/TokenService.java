package com.byeolnight.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redisTemplate;

    public void delete(String refreshToken, String email) {
        redisTemplate.delete("refresh:" + email);
        System.out.println("[Logout] refreshToken for " + email + " removed");
    }

    public void saveRefreshToken(String email, String refreshToken, long expirationMillis) {
        redisTemplate.opsForValue().set("refresh:" + email, refreshToken, expirationMillis, TimeUnit.MILLISECONDS);
    }

    public boolean isValidRefreshToken(String email, String refreshToken) {
        String stored = redisTemplate.opsForValue().get("refresh:" + email);
        return stored != null && stored.equals(refreshToken);
    }
}
