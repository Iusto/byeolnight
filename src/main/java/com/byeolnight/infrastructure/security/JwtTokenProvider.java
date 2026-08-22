package com.byeolnight.infrastructure.security;

import com.byeolnight.entity.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final Duration accessTokenExpiry = Duration.ofMinutes(30);
    private final Duration refreshTokenExpiry = Duration.ofDays(7);

    public JwtTokenProvider(@Value("${app.security.jwt.secret}") String secret) {
        validateSecret(secret);
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    static void validateSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT 시크릿이 설정되지 않았습니다.");
        }
        if (secret.startsWith("{cipher}") || secret.startsWith("invalid") || secret.contains("${")) {
            throw new IllegalStateException("JWT 시크릿이 정상적으로 복호화되지 않았습니다.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT 시크릿은 UTF-8 기준 32바이트 이상이어야 합니다.");
        }
    }

    /** JWT 생성만 담당하며 Refresh Token 저장은 TokenService가 담당한다. */
    public String[] generateTokens(User user) {
        return new String[]{generateAccessToken(user), generateRefreshToken(user)};
    }

    private String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(accessTokenExpiry)))
                .signWith(key)
                .compact();
    }

    private String generateRefreshToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("type", "refresh")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(refreshTokenExpiry)))
                .signWith(key)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }
    
    private Claims parseToken(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }

    // 호환성 메서드들
    public boolean validate(String token) {
        return validateAccessToken(token);
    }

    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getEmail(String token) {
        return parseToken(token).get("email", String.class);
    }

    public String createAccessToken(User user) {
        return generateAccessToken(user);
    }

    public String createRefreshToken(User user) {
        return generateRefreshToken(user);
    }

    public long getRefreshTokenValidity() {
        return refreshTokenExpiry.toMillis();
    }

    public long getExpiration(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration().getTime() - System.currentTimeMillis();
    }

    public Authentication getAuthentication(String token) {
        try {
            Claims claims = parseToken(token);
            Long userId = Long.parseLong(claims.getSubject());
            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class);

            User principal = User.builder()
                    .id(userId)
                    .email(email)
                    .role(User.Role.valueOf(role))
                    .build();

            return new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
        } catch (Exception e) {
            log.error("토큰에서 Authentication 생성 실패: {}", e.getMessage());
            return null;
        }
    }

    public Long getUserIdFromRequest(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token != null && validate(token)) {
            return getUserIdFromToken(token);
        }
        return null;
    }

    private String resolveToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

}
