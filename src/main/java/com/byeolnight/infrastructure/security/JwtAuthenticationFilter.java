package com.byeolnight.infrastructure.security;

import com.byeolnight.service.auth.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 토큰 기반 인증 필터
 *
 * 역할:
 * - 모든 HTTP 요청에서 JWT 토큰 추출 및 검증
 * - 화이트리스트 경로는 인증 없이 통과 허용
 * - 블랙리스트 토큰 차단
 * - 인증 성공 시 SecurityContext에 인증 정보 설정
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final TokenService tokenService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   UserDetailsService userDetailsService,
                                   TokenService tokenService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.tokenService = tokenService;
    }

    private boolean isWhitelisted(String uri) {
        for (String pattern : AuthWhitelist.PATHS) {
            if (pathMatcher.match(pattern, uri)) return true;
        }
        return false;
    }
    
    private boolean isLowPriorityPath(String uri) {
        return uri.contains("/health") || 
               uri.contains("/actuator") || 
               uri.contains("/favicon.ico") || 
               uri.contains("/chat/ban-status") ||
               uri.contains("/api/member/users/me") ||
               uri.contains("/oauth2/authorization") ||
               uri.equals("/") ||
               uri.contains("/.env") ||
               uri.contains("/.git") ||
               uri.contains("/wp-admin") ||
               uri.contains("/phpmyadmin") ||
               uri.contains("/phpunit") ||
               uri.contains("/vendor") ||
               uri.contains("/laravel") ||
               uri.contains("/eval-stdin") ||
               uri.contains("/hello.world");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (isWhitelisted(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 쿠키에서 Access Token 추출
        String token = null;
        Cookie[] cookies = request.getCookies();
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName()) && 
                    cookie.getValue() != null && !cookie.getValue().trim().isEmpty()) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        


        if (token == null) {
            handleMissingToken(uri, response);
            return;
        }

        if (!jwtTokenProvider.validate(token)) {
            log.warn("❌ JWT 유효성 검사 실패: {}", uri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (tokenService.isAccessTokenBlacklisted(token)) {
            log.warn("❌ 블랙리스트 토큰 차단: {}", uri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String userId = jwtTokenProvider.getEmail(token);
        if (userId == null) {
            log.error("❌ 토큰에서 사용자 ID 추출 실패: {}", uri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
            setAuthentication(request, userDetails);
            log.debug("✅ 인증 성공: {} - {}", userId, uri);
        } catch (UsernameNotFoundException e) {
            log.error("❌ 사용자 조회 실패: {} - {}", userId, e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
    
    private void handleMissingToken(String uri, HttpServletResponse response) {
        if (isLowPriorityPath(uri)) {
            log.debug("인증 불필요 요청: {}", uri);
        } else {
            log.warn("❌ 토큰 부재: {}", uri);
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
    
    private void setAuthentication(HttpServletRequest request, UserDetails userDetails) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
