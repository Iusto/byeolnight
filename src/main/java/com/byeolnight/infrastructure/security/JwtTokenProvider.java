package com.byeolnight.infrastructure.security;

import com.byeolnight.domain.entity.user.User;
import com.byeolnight.service.user.CustomUserDetailsService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Duration;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final CustomUserDetailsService customUserDetailsService;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-validity}")
    private Duration accessTokenValidity;

    @Value("${app.jwt.refresh-token-validity}")
    private Duration refreshTokenValidity;

    // JWT 서명용 키 생성
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * AccessToken 생성
     */
    public String createAccessToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole().name())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenValidity.toMillis()))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * RefreshToken 생성
     */
    public String createRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenValidity.toMillis()))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validate(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("⏰ JWT 만료됨: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("❌ JWT 검증 실패: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("❓ 기타 JWT 예외: {}", e.getMessage());
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

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public long getExpiration(String token) {
        return extractAllClaims(token).getExpiration().getTime() - System.currentTimeMillis();
    }

    /**
     * 토큰 기반 인증 객체 생성
     */
    public Authentication getAuthentication(String token) {
        String email = getEmail(token);
        if (email == null) throw new RuntimeException("이메일 추출 실패");

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    /**
     * refresh 토큰 유효기간 getter
     */
    public long getRefreshTokenValidity() {
        return refreshTokenValidity.toMillis();
    }
}
