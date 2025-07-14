package com.byeolnight.infrastructure.security;

import com.byeolnight.service.auth.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        log.debug("🔍 요청 URI: {} {}", method, uri);

        if (isWhitelisted(uri)) {
            log.debug("✅ 화이트리스트 경로, 인증 없이 통과");

            // ✅ 토큰이 있더라도 화이트리스트는 무조건 통과
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);
        log.debug("🪪 추출된 토큰: {}", token);

        if (token == null) {
            log.warn("❌ Authorization 헤더 없음 또는 형식 오류");
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
        log.debug("🔑 사용자 권한: {}", userDetails.getAuthorities());
        
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        log.debug("✅ 인증 성공: {} (권한: {})", email, userDetails.getAuthorities());

        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
