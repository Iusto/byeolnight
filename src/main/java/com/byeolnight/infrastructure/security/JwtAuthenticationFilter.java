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

    /**
     * 화이트리스트 경로는 인증 없이 통과
     */
    private boolean isWhitelisted(String uri) {
        for (String pattern : AuthWhitelist.PATHS) {
            if (pathMatcher.match(pattern, uri)) return true;
        }
        return false;
    }
    
    /**
     * 로그 레벨을 낮춰야 하는 경로인지 확인
     */
    private boolean isLowLogLevel(String uri) {
        return uri.contains("/health") || 
               uri.contains("/actuator") || 
               uri.contains("/favicon.ico") || 
               uri.contains("/chat/ban-status") ||
               uri.contains("/api/member/users/me") ||
               uri.contains("/oauth2/authorization");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        log.info("🔍 요청 URI: {} {}", method, uri);

        if (isWhitelisted(uri)) {
            log.info("✅ 화이트리스트 경로, 인증 없이 통과");

            // ✅ 토큰이 있더라도 화이트리스트는 무조건 통과
            filterChain.doFilter(request, response);
            return;
        }

        // 쿠키에서 Access Token 추출
        String token = null;
        Cookie[] cookies = request.getCookies();
        
        log.info("🍪 쿠키 상태: {}", cookies != null ? cookies.length + "개 쿠키 있음" : "쿠키 없음");
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String cookieValue = cookie.getValue();
                log.info("🍪 쿠키: {} = {}", cookie.getName(), 
                    cookieValue != null && cookieValue.length() > 10 ? 
                    cookieValue.substring(0, 10) + "..." : cookieValue);
                
                if ("accessToken".equals(cookie.getName()) && 
                    cookieValue != null && !cookieValue.trim().isEmpty()) {
                    token = cookieValue;
                    log.info("✅ accessToken 쿠키에서 발견: {}", token.substring(0, 10) + "...");
                    break;
                }
            }
        }
        
        // 쿠키에서 토큰을 찾지 못한 경우, 헤더에서 추출 시도 (후방 호환성)
        if (token == null) {
            token = SecurityUtils.resolveToken(request);
            log.info("헤더에서 토큰 추출 시도: {}", token != null ? "성공" : "실패");
        }
        
        log.info("🪪 추출된 토큰: {}", token != null ? "존재 (길이: " + token.length() + ")" : "없음");

        if (token == null) {
            // 특정 경로는 로그 레벨 낮춤
            if (isLowLogLevel(uri)) {
                log.debug("인증 불필요 또는 예상된 요청: {}", uri);
            } else {
                log.warn("❌ 토큰이 없음 (쿠키 및 헤더 모두 부재): {}", uri);
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (!jwtTokenProvider.validate(token)) {
            log.warn("❌ JWT 유효성 검사 실패");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (tokenService.isAccessTokenBlacklisted(token)) {
            log.warn("❌ 블랙리스트 토큰 접근 차단됨");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String email = jwtTokenProvider.getEmail(token);
        if (email == null) {
            log.error("❌ 토큰에서 이메일 추출 실패");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        log.info("🔑 사용자 권한: {}", userDetails.getAuthorities());
        
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        log.info("✅ 인증 성공: {} (권한: {})", email, userDetails.getAuthorities());

        filterChain.doFilter(request, response);
    }


}
