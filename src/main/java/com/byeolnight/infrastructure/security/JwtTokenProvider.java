package com.byeolnight.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.byeolnight.domain.entity.user.User;

import java.security.Key;
import java.util.Date;

/**
 * JWT 생성 및 검증을 담당하는 유틸리티 클래스
 */
@Slf4j
@Component
public class JwtTokenProvider {

    // 서명에 사용할 비밀키
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // 유효시간
    private final long ACCESS_TOKEN_VALIDITY = 1000 * 60 * 30; // 30분
    private final long REFRESH_TOKEN_VALIDITY = 1000L * 60 * 60 * 24 * 7; // 7일

    /**
     * AccessToken 생성
     */
    public String createAccessToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole().name())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY))
                .signWith(key)
                .compact();
    }

    /**
     * RefreshToken 생성
     */
    public String createRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_VALIDITY))
                .signWith(key)
                .compact();
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validate(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token); // 이 줄에서 유효성 검사 수행
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("⏰ JWT 만료됨: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("❌ JWT 검증 실패: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("❓ 기타 JWT 예외: {}", e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        return false;
    }

    public boolean validateRefreshToken(String token) {
        return validate(token);
    }

    /**
     * 토큰에서 이메일 추출
     */
    public String getEmail(String token) {
        try {
            return extractAllClaims(token).getSubject();
        } catch (Exception e) {
            log.warn("❌ 이메일 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Claims 전체 추출
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 만료 시간 계산
     */
    public long getExpiration(String token) {
        return extractAllClaims(token).getExpiration().getTime() - System.currentTimeMillis();
    }
}
