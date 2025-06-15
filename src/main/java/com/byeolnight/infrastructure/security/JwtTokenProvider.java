package com.byeolnight.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import com.byeolnight.domain.entity.user.User;

import java.security.Key;
import java.util.Date;

/**
 * JWT 생성 및 검증을 담당하는 유틸리티 클래스
 * - AccessToken과 RefreshToken을 생성하고,
 * - 토큰 유효성 검증, Claim 파싱, 만료시간 계산 등을 제공
 *
 * ✅ AccessToken: 인증 처리 (30분)
 * ✅ RefreshToken: 재발급 처리 (7일)
 *
 * ⚠️ 서버는 토큰 자체에 상태를 저장하지 않기 때문에,
 * 로그아웃/보안 대응에는 블랙리스트(Redis)가 반드시 함께 사용되어야 함
 */
@Component
public class JwtTokenProvider {

    // 비밀키 (HS256 알고리즘 기반으로 생성)
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // AccessToken 유효 시간: 30분
    private final long ACCESS_TOKEN_VALIDITY = 1000 * 60 * 30;

    // RefreshToken 유효 시간: 7일
    private final long REFRESH_TOKEN_VALIDITY = 1000L * 60 * 60 * 24 * 7;

    /**
     * AccessToken 생성
     * - subject: 사용자 이메일
     * - claim: 사용자 역할 정보 포함
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
     * - subject: 사용자 이메일
     * - claim 없이 단순 만료 정보만 포함
     */
    public String createRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_VALIDITY))
                .signWith(key)
                .compact();
    }

    /**
     * 토큰 유효성 검증 (서명 위조 / 만료 여부)
     */
    public boolean validate(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * RefreshToken도 동일한 방식으로 검증
     */
    public boolean validateRefreshToken(String token) {
        return validate(token);
    }

    /**
     * 토큰에서 사용자 이메일(subject)을 추출
     */
    public String getEmail(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    /**
     * 모든 Claims (subject, expiration, custom claims 등)를 추출
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
    }

    /**
     * AccessToken의 남은 만료 시간을 계산하여 반환 (ms 단위)
     * - 로그아웃 시 블랙리스트 등록에 사용됨
     * - TTL 설정을 통해 Redis에서 자동 삭제 유도 가능
     */
    public long getExpiration(String token) {
        Date expiration = extractAllClaims(token).getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }
}
