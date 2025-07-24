package com.byeolnight.infrastructure.security;

import com.byeolnight.domain.entity.user.User;
import com.byeolnight.service.user.CustomUserDetailsService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 토큰 생성, 검증, 파싱 담당 프로바이더
 *
 * 역할:
 * - Access Token 및 Refresh Token 생성 (30분/7일 유효기간)
 * - JWT 토큰 서명 및 검증
 * - 토큰에서 사용자 정보(이메일, 권한) 추출
 * - Spring Security Authentication 객체 생성
 * - HTTP 요청에서 사용자 ID 추출 지원
 */
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
    
    @Value("${app.jwt.allowed-clock-skew:5m}")
    private Duration allowedClockSkew;

    // JWT 서명용 키 생성
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * AccessToken 생성
     * 
     * 명시적으로 UTC 시간을 사용하여 토큰 생성
     */
    public String createAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusMillis(accessTokenValidity.toMillis());
        
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole().name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiryDate))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * RefreshToken 생성
     * 
     * 명시적으로 UTC 시간을 사용하여 토큰 생성
     */
    public String createRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusMillis(refreshTokenValidity.toMillis());
        
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiryDate))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validate(String token) {
        try {
            // 설정에서 가져온 시간 오차(clock skew) 허용 설정 적용
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .setAllowedClockSkewSeconds(allowedClockSkew.getSeconds()) // 설정에서 가져온 시간 오차 허용
                .build()
                .parseClaimsJws(token);
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
    
    /**
     * Refresh Token 유효성 검증
     * 
     * 일반 토큰 검증과 동일한 방식으로 시간 오차(clock skew)를 허용하여 검증합니다.
     */
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
                .setAllowedClockSkewSeconds(allowedClockSkew.getSeconds()) // 설정에서 가져온 시간 오차 허용
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public long getExpiration(String token) {
        return extractAllClaims(token).getExpiration().getTime() - System.currentTimeMillis();
    }

    /**
     * 토큰의 남은 유효시간 반환 (밀리초)
     */
    public long getRemainingTime(String token) {
        try {
            return extractAllClaims(token).getExpiration().getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            log.warn("❌ 토큰 남은 시간 계산 실패: {}", e.getMessage());
            return 0;
        }
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

    /**
     * HTTP 요청에서 사용자 ID 추출
     */
    public Long getUserIdFromRequest(HttpServletRequest request) {
        String token = SecurityUtils.resolveToken(request);
        if (token != null && validate(token)) {
            String email = getEmail(token);
            if (email != null) {
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
                if (userDetails instanceof User) {
                    return ((User) userDetails).getId();
                }
            }
        }
        throw new RuntimeException("유효하지 않은 토큰입니다.");
    }


}
