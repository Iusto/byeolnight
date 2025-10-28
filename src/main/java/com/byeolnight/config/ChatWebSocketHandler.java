package com.byeolnight.config;

import com.byeolnight.dto.chat.ChatMessageDto;
import com.byeolnight.entity.user.User;
import com.byeolnight.service.chat.AdminChatService;
import com.byeolnight.service.chat.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final AdminChatService adminChatService;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Authentication auth = (Authentication) session.getAttributes().get("authentication");
        if (auth != null && auth.getPrincipal() instanceof User user) {
            sessions.put(user.getNickname(), session);
            log.info("✅ WebSocket 연결: {}", user.getNickname());
        } else {
            log.warn("❌ 인증되지 않은 WebSocket 연결 시도 차단");
            try {
                session.close(CloseStatus.NOT_ACCEPTABLE);
            } catch (IOException e) {
                log.error("세션 종료 실패", e);
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Authentication auth = (Authentication) session.getAttributes().get("authentication");
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return;
        }

        ChatMessageDto chatMessage = objectMapper.readValue(message.getPayload(), ChatMessageDto.class);
        chatMessage.setSender(user.getNickname());

        // 채팅 금지 확인
        if (adminChatService.isUserBanned(user.getNickname())) {
            sendToUser(user.getNickname(), Map.of("error", "채팅이 제한되어 메시지를 보낼 수 없습니다."));
            return;
        }

        // IP 추출
        String clientIp = (String) session.getAttributes().getOrDefault("clientIp", "unknown");
        
        // 저장 및 브로드캐스트
        chatService.save(chatMessage, clientIp);
        broadcast(chatMessage);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Authentication auth = (Authentication) session.getAttributes().get("authentication");
        if (auth != null && auth.getPrincipal() instanceof User user) {
            sessions.remove(user.getNickname());
            log.info("❌ WebSocket 연결 종료: {}", user.getNickname());
        }
    }

    private void broadcast(ChatMessageDto message) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("메시지 직렬화 실패", e);
            return;
        }

        sessions.values().forEach(session -> {
            try {
                session.sendMessage(new TextMessage(payload));
            } catch (IOException e) {
                log.error("메시지 전송 실패", e);
            }
        });
    }

    private void sendToUser(String nickname, Object message) {
        WebSocketSession session = sessions.get(nickname);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (IOException e) {
                log.error("사용자 메시지 전송 실패", e);
            }
        }
    }
}
