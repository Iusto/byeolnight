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
        log.info("[Logout] refreshToken for {} removed", email);
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
     * OAuth2 신규 계정 진행 플래그 저장 (5분 TTL)
     * - STATELESS 환경에서 세션 대신 Redis 사용
     */
    public void saveSkipRecoveryFlag(String email) {
        redisTemplate.opsForValue().set("skip_recovery:" + email, "true", 5, TimeUnit.MINUTES);
    }

    /**
     * OAuth2 신규 계정 진행 플래그 조회 후 즉시 삭제 (1회용)
     */
    public boolean getAndDeleteSkipRecoveryFlag(String email) {
        String key = "skip_recovery:" + email;
        String value = redisTemplate.opsForValue().get(key);
        if ("true".equals(value)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    /**
     * AccessToken을 블랙리스트에 등록
     */
    public void blacklistAccessToken(String accessToken, long expirationMillis) {
        String key = getBlacklistKey(accessToken);
        redisTemplate.opsForValue().set(key, "true", expirationMillis, TimeUnit.MILLISECONDS);
        log.info("🚫 블랙리스트 등록됨: {}, TTL: {}ms", key, expirationMillis);
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
            // SHA-256은 JVM에서 항상 지원되므로 실질적으로 도달 불가능한 경로
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
