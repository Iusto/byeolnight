package com.byeolnight.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redisTemplate;

    public void delete(String refreshToken, String email) {
        redisTemplate.delete("refresh:" + email);
        System.out.println("[Logout] refreshToken for " + email + " removed");
    }
    
    public void deleteRefreshToken(String email) {
        redisTemplate.delete("refresh:" + email);
        log.info("Refresh Token 삭제 완료: {}", email);
    }

    public void saveRefreshToken(String email, String refreshToken, long expirationMillis) {
        redisTemplate.opsForValue().set("refresh:" + email, refreshToken, expirationMillis, TimeUnit.MILLISECONDS);
    }

    public boolean isValidRefreshToken(String email, String refreshToken) {
        String stored = redisTemplate.opsForValue().get("refresh:" + email);
        return stored != null && stored.equals(refreshToken);
    }

    /**
     * AccessToken을 블랙리스트에 등록
     */
    public void blacklistAccessToken(String accessToken, long expirationMillis) {
        String key = getBlacklistKey(accessToken);
        redisTemplate.opsForValue().set(key, "true", expirationMillis, TimeUnit.MILLISECONDS);
        System.out.println("🚫 블랙리스트 등록됨: " + key);
    }

    /**
     * AccessToken이 블랙리스트에 있는지 확인
     */
    public boolean isAccessTokenBlacklisted(String accessToken) {
        String key = getBlacklistKey(accessToken);
        log.debug("🧪 블랙리스트 검사 키: {}", key); // 추가
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 토큰을 해싱하여 Redis 키로 사용
     */
    private String getBlacklistKey(String token) {
        return "blacklist:" + hashToken(token);
    }

    /**
     * SHA-256 + Base64 인코딩 방식 (JWT가 너무 길어서 Redis 키가 잘리지않게)
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Token 해싱 실패", e);
        }
    }
}
