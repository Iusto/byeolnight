package com.byeolnight.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * WebSocket 연결 시 JWT 인증 처리 인터셉터
 *
 * 역할:
 * - WebSocket CONNECT 시 JWT 토큰 검증
 * - 인증된 사용자는 인증 정보 설정, 비로그인도 연결 허용
 * - 클라이언트 IP 주소 추출 및 세션에 저장
 * - 실시간 채팅 및 알림 시스템에서 사용
 */
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 클라이언트 IP 추출 및 세션에 저장
            try {
                String clientIp = extractClientIpFromHeaders(accessor);
                accessor.getSessionAttributes().put("clientIp", clientIp);
            } catch (Exception e) {
                System.out.println("클라이언트 IP 추출 실패: " + e.getMessage());
            }
            
            // 1. JWT 토큰 기반 인증 시도
            String token = extractTokenFromHeader(accessor);
            if (token == null) {
                token = extractTokenFromCookie(accessor);
            }
            
            boolean authenticated = false;
            
            if (token != null) {
                try {
                    if (jwtTokenProvider.validate(token)) {
                        Authentication auth = jwtTokenProvider.getAuthentication(token);
                        accessor.setUser(auth);
                        authenticated = true;
                        System.out.println("WebSocket JWT 인증 성공: " + auth.getName());
                    }
                } catch (Exception e) {
                    System.out.println("WebSocket JWT 토큰 검증 실패: " + e.getMessage());
                }
            }
            
            // 2. JWT 인증 실패 시 닉네임 헤더 기반 세션 인증 시도
            if (!authenticated) {
                String userNickname = accessor.getFirstNativeHeader("X-User-Nickname");
                if (userNickname != null && !userNickname.trim().isEmpty()) {
                    // 간단한 사용자 인증 객체 생성 (세션 기반)
                    Authentication sessionAuth = createSessionAuthentication(userNickname);
                    accessor.setUser(sessionAuth);
                    authenticated = true;
                    System.out.println("WebSocket 세션 기반 인증 성공: " + userNickname);
                }
            }
            
            if (!authenticated) {
                System.out.println("WebSocket 연결 - 인증 실패, 비로그인 사용자로 연결");
            }
        }

        // 연결 성공 로그
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            System.out.println("WebSocket CONNECT 완료 - User: " + 
                (accessor.getUser() != null ? accessor.getUser().getName() : "비로그인"));
        }
        
        return message;
    }
    
    private String extractTokenFromHeader(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
    
    private String extractTokenFromCookie(StompHeaderAccessor accessor) {
        // SockJS는 브라우저 쿠키를 자동으로 전달하지 않으므로 
        // 클라이언트에서 Authorization 헤더로 전달된 토큰을 우선 확인
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            System.out.println("Authorization 헤더에서 토큰 발견");
            return authHeader.substring(7);
        }
        
        // 쿠키 헤더 확인 (SockJS 환경에서는 제한적)
        String cookieHeader = accessor.getFirstNativeHeader("Cookie");
        System.out.println("WebSocket Cookie 헤더: " + cookieHeader);
        
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                String[] parts = cookie.trim().split("=", 2);
                if (parts.length == 2 && "accessToken".equals(parts[0])) {
                    String token = parts[1];
                    System.out.println("accessToken 쿠키 발견: " + token.substring(0, Math.min(20, token.length())) + "...");
                    return token;
                }
            }
        }
        
        System.out.println("토큰을 찾을 수 없습니다 - Authorization 헤더와 쿠키 모두 확인했으나 없음");
        return null;
    }
    
    private String extractClientIpFromHeaders(StompHeaderAccessor accessor) {
        // WebSocket 헤더에서 IP 추출 (IpUtil과 동일한 우선순위)
        String[] headers = {
            "X-Client-IP",
            "X-Forwarded-For", 
            "X-Real-IP", 
            "Proxy-Client-IP"
        };
        
        for (String header : headers) {
            String ip = accessor.getFirstNativeHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.contains(",") ? ip.split(",")[0].trim() : ip;
            }
        }
        
        return "unknown";
    }
    
    private Authentication createSessionAuthentication(String nickname) {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            nickname, null, java.util.Collections.emptyList());
    }
}
