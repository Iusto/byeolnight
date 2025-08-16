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
            
            // 토큰 추출: 핸드셰이크 세션 -> Authorization 헤더 -> 쿠키 순서
            String token = (String) accessor.getSessionAttributes().get("token");
            if (token == null) {
                token = extractTokenFromHeader(accessor);
            }
            if (token == null) {
                token = extractTokenFromCookie(accessor);
            }
            
            System.out.println("토큰 추출 결과: " + (token != null ? "성공" : "실패"));
            
            System.out.println("WebSocket 연결 - 토큰: " + (token != null ? "존재" : "없음"));
            
            if (token != null) {
                try {
                    if (jwtTokenProvider.validate(token)) {
                        Authentication auth = jwtTokenProvider.getAuthentication(token);
                        accessor.setUser(auth);
                        System.out.println("WebSocket 인증 성공: " + auth.getName());
                    } else {
                        System.out.println("WebSocket 토큰 검증 실패: 유효하지 않은 토큰");
                        // 유효하지 않은 토큰이라도 비로그인 사용자로 연결 허용
                    }
                } catch (Exception e) {
                    System.out.println("WebSocket 토큰 검증 실패: " + e.getMessage());
                    // 예외 발생 시도 비로그인 사용자로 연결 허용
                }
            } else {
                System.out.println("WebSocket 연결 - 토큰 없음, 비로그인 사용자로 연결");
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
        
        // 쿠키에서 찾지 못한 경우 추가 헤더 확인
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            System.out.println("Authorization 헤더에서 토큰 발견");
            return authHeader.substring(7);
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
    
    private String extractTokenFromUrl(StompHeaderAccessor accessor) {
        try {
            // WebSocket URL에서 토큰 추출
            String destination = accessor.getDestination();
            if (destination != null && destination.contains("token=")) {
                String[] parts = destination.split("token=");
                if (parts.length > 1) {
                    String token = parts[1].split("&")[0]; // 다른 파라미터가 있을 경우 처리
                    System.out.println("URL 파라미터에서 토큰 발견: " + token.substring(0, Math.min(20, token.length())) + "...");
                    return java.net.URLDecoder.decode(token, "UTF-8");
                }
            }
            
            // SockJS 연결 정보에서 토큰 추출 시도
            Object nativeHeaders = accessor.getSessionAttributes().get("token");
            if (nativeHeaders != null) {
                System.out.println("세션에서 토큰 발견");
                return nativeHeaders.toString();
            }
        } catch (Exception e) {
            System.out.println("URL 토큰 추출 실패: " + e.getMessage());
        }
        return null;
    }
}
