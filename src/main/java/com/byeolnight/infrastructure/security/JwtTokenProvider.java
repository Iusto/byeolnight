package com.byeolnight.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final StringRedisTemplate redisTemplate;
    private final Duration accessTokenExpiry = Duration.ofMinutes(30);
    private final Duration refreshTokenExpiry = Duration.ofDays(7);

    public JwtTokenProvider(@Value("${app.security.jwt.secret}") String secret, StringRedisTemplate redisTemplate) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.redisTemplate = redisTemplate;
    }

    public String[] generateTokens(Long userId, String clientInfo, String ipAddress) {
        String sessionId = generateSessionId(userId, clientInfo, ipAddress);
        
        String accessToken = generateAccessToken(userId, sessionId);
        String refreshToken = generateRefreshToken(userId, sessionId);
        
        // Redis에 세션 저장
        String sessionKey = "session:" + sessionId;
        redisTemplate.opsForValue().set(sessionKey, refreshToken, refreshTokenExpiry);
        
        return new String[]{accessToken, refreshToken, sessionId};
    }

    private String generateAccessToken(Long userId, String sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("sessionId", sessionId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(accessTokenExpiry)))
                .signWith(key)
                .compact();
    }

    private String generateRefreshToken(Long userId, String sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("sessionId", sessionId)
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

    public String getSessionIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("sessionId", String.class);
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
        return getUserIdFromToken(token).toString();
    }

    public String createAccessToken(com.byeolnight.entity.user.User user) {
        return generateAccessToken(user.getId(), UUID.randomUUID().toString());
    }

    public String createRefreshToken(com.byeolnight.entity.user.User user) {
        return generateRefreshToken(user.getId(), UUID.randomUUID().toString());
    }

    public long getRefreshTokenValidity() {
        return refreshTokenExpiry.toMillis();
    }

    public long getExpiration(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration().getTime() - System.currentTimeMillis();
    }

    public org.springframework.security.core.Authentication getAuthentication(String token) {
        try {
            Long userId = getUserIdFromToken(token);
            
            // UserService를 통해 User 조회
            com.byeolnight.service.user.UserService userService = 
                com.byeolnight.infrastructure.config.ApplicationContextProvider.getBean(com.byeolnight.service.user.UserService.class);
            
            com.byeolnight.entity.user.User user = userService.findById(userId);
            
            // UsernamePasswordAuthenticationToken 생성
            return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                user, null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );
        } catch (Exception e) {
            log.error("토큰에서 Authentication 생성 실패: {}", e.getMessage());
            return null;
        }
    }

    public Long getUserIdFromRequest(jakarta.servlet.http.HttpServletRequest request) {
        String token = resolveToken(request);
        if (token != null && validate(token)) {
            return getUserIdFromToken(token);
        }
        return null;
    }

    private String resolveToken(jakarta.servlet.http.HttpServletRequest request) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private String generateSessionId(Long userId, String clientInfo, String ipAddress) {
        try {
            String data = userId + ":" + clientInfo + ":" + ipAddress + ":" + System.currentTimeMillis();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}