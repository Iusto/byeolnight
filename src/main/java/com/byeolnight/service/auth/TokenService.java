package com.byeolnight.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 로그아웃 시 RefreshToken 제거
     * 클라이언트에서 더 이상 재발급 요청이 불가능하게 한다.
     */
    public void delete(String refreshToken, String email) {
        redisTemplate.delete("refresh:" + email);
        System.out.println("[Logout] refreshToken for " + email + " removed");
    }

    /**
     * 로그인 또는 재발급 시 RefreshToken 저장
     * TTL을 설정하여 자동 만료 처리함
     */
    public void saveRefreshToken(String email, String refreshToken, long expirationMillis) {
        redisTemplate.opsForValue().set("refresh:" + email, refreshToken, expirationMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 클라이언트에서 보낸 RefreshToken이 유효한지 검증
     */
    public boolean isValidRefreshToken(String email, String refreshToken) {
        String stored = redisTemplate.opsForValue().get("refresh:" + email);
        return stored != null && stored.equals(refreshToken);
    }

    /**
     * AccessToken을 블랙리스트에 등록
     * 주로 로그아웃, 비밀번호 변경, 보안 사고 대응 시 사용
     * expirationMillis: 토큰 만료 시간 기준으로 Redis TTL 설정
     */
    public void blacklistAccessToken(String accessToken, long expirationMillis) {
        redisTemplate.opsForValue().set("blacklist:" + accessToken, "true", expirationMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 현재 AccessToken이 블랙리스트에 등록되었는지 확인
     * 등록되어 있다면 인증을 차단해야 함
     */
    public boolean isAccessTokenBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + accessToken));
    }
}
