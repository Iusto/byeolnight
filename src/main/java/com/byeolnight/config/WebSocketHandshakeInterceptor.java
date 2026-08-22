package com.byeolnight.config;

import com.byeolnight.infrastructure.security.JwtTokenProvider;
import com.byeolnight.service.auth.TokenService;
import com.byeolnight.service.user.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket Handshake 시 HttpOnly 쿠키에서 JWT 토큰 추출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final TokenService tokenService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, 
                                 ServerHttpResponse response,
                                 WebSocketHandler wsHandler, 
                                 Map<String, Object> attributes) throws Exception {
        
        // HTTP 쿠키에서 accessToken 추출
        String token = extractTokenFromCookies(request);
        
        if (token != null && jwtTokenProvider.validate(token)
                && !tokenService.isAccessTokenBlacklisted(token)) {
            try {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                UserDetails principal = userDetailsService.loadUserByUsername(userId.toString());
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                attributes.put("authentication", authentication);
                attributes.put("accessToken", token);
                log.debug("✅ WebSocket Handshake 인증 성공: {}", authentication.getName());
                return true;
            } catch (Exception e) {
                log.debug("❌ WebSocket Handshake 토큰 검증 실패: {}", e.getMessage());
            }
        }
        
        // 토큰이 없거나 유효하지 않아도 연결 허용 (비로그인 사용자)
        log.debug("🔓 WebSocket Handshake - 비로그인 사용자 연결 허용");
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, 
                             ServerHttpResponse response,
                             WebSocketHandler wsHandler, 
                             Exception exception) {
        // 후처리 로직 (필요시)
    }

    private String extractTokenFromCookies(ServerHttpRequest request) {
        String cookieHeader = request.getHeaders().getFirst("Cookie");
        if (cookieHeader == null) return null;
        
        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
            String[] parts = cookie.trim().split("=", 2);
            if (parts.length == 2 && "accessToken".equals(parts[0])) {
                return parts[1];
            }
        }
        return null;
    }
}
