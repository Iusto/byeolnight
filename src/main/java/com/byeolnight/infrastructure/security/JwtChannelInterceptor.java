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
import org.springframework.util.StringUtils;

/**
 * WebSocket 연결 시 JWT 인증 처리 인터셉터
 * HttpOnly 쿠키 기반 인증 및 클라이언트 IP 추출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private static final String[] IP_HEADERS = {
        "X-Client-IP", "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP"
    };
    private static final String UNKNOWN_IP = "unknown";
    private static final String CLIENT_IP_KEY = "clientIp";
    private static final String AUTH_KEY = "authentication";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleWebSocketConnect(accessor);
        }
        
        return message;
    }
    
    private void handleWebSocketConnect(StompHeaderAccessor accessor) {
        // 클라이언트 IP 추출 및 저장
        String clientIp = extractClientIp(accessor);
        accessor.getSessionAttributes().put(CLIENT_IP_KEY, clientIp);
        
        // Handshake에서 설정된 인증 정보 사용
        Authentication handshakeAuth = (Authentication) accessor.getSessionAttributes().get(AUTH_KEY);
        
        if (handshakeAuth != null) {
            accessor.setUser(handshakeAuth);
            log.debug("✅ WebSocket 인증 성공: {}", handshakeAuth.getName());
        } else {
            log.debug("🔓 WebSocket 비로그인 연결");
        }
        
        log.debug("WebSocket CONNECT - User: {}, IP: {}", 
            handshakeAuth != null ? handshakeAuth.getName() : "비로그인", clientIp);
    }
    
    private String extractClientIp(StompHeaderAccessor accessor) {
        for (String header : IP_HEADERS) {
            String ip = accessor.getFirstNativeHeader(header);
            if (StringUtils.hasText(ip) && !UNKNOWN_IP.equalsIgnoreCase(ip)) {
                return ip.contains(",") ? ip.split(",")[0].trim() : ip;
            }
        }
        return UNKNOWN_IP;
    }
}
