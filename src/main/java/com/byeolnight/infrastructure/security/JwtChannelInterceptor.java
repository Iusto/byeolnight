package com.byeolnight.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
                // IP 추출 실패는 심각한 문제가 아니므로 DEBUG 레벨로 처리
            }
            
            // Handshake에서 설정된 인증 정보 사용 (HttpOnly 쿠키 기반)
            Authentication handshakeAuth = (Authentication) accessor.getSessionAttributes().get("authentication");
            
            if (handshakeAuth != null) {
                accessor.setUser(handshakeAuth);
                log.debug("✅ WebSocket 인증 성공 (Handshake): {}", handshakeAuth.getName());
            } else {
                log.debug("🔓 WebSocket 비로그인 사용자 연결");
            }
        }

        // 연결 성공 로그
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.debug("WebSocket CONNECT 완료 - User: {}", 
                (accessor.getUser() != null ? accessor.getUser().getName() : "비로그인"));
        }
        
        return message;
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
