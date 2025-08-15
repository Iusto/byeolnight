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
            
            // HttpOnly 쿠키에서 토큰 추출
            String token = extractTokenFromCookie(accessor);
            System.out.println("WebSocket 연결 - 토큰: " + (token != null ? "존재" : "없음"));
            
            if (token != null) {
                try {
                    if (jwtTokenProvider.validate(token)) {
                        Authentication auth = jwtTokenProvider.getAuthentication(token);
                        accessor.setUser(auth);
                        System.out.println("WebSocket 인증 성공: " + auth.getName());
                    } else {
                        System.out.println("WebSocket 토큰 검증 실패: 유효하지 않은 토큰");
                    }
                } catch (Exception e) {
                    // 토큰 검증 실패 시 로그만 출력하고 연결은 허용
                    System.out.println("WebSocket 토큰 검증 실패: " + e.getMessage());
                }
            } else {
                System.out.println("WebSocket 연결 - 토큰 없음, 비로그인 사용자로 연결");
            }
            // 토큰이 없거나 유효하지 않아도 비로그인 사용자로 연결 허용
        }

        return message;
    }
    
    private String extractTokenFromCookie(StompHeaderAccessor accessor) {
        String cookieHeader = accessor.getFirstNativeHeader("Cookie");
        System.out.println("WebSocket Cookie 헤더: " + cookieHeader);
        
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                String[] parts = cookie.trim().split("=", 2);
                if (parts.length == 2 && "accessToken".equals(parts[0])) {
                    System.out.println("accessToken 쿠키 발견: " + parts[1].substring(0, Math.min(20, parts[1].length())) + "...");
                    return parts[1];
                }
            }
        }
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
}
