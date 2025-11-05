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
            log.info("🔓 WebSocket 연결: 비로그인 사용자 (읽기 전용)");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Authentication auth = (Authentication) session.getAttributes().get("authentication");
        
        // ping 메시지 처리 (pong 응답)
        if (payload.contains("\"type\":\"ping\"")) {
            session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
            return;
        }
        
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return;
        }

        ChatMessageDto chatMessage = objectMapper.readValue(payload, ChatMessageDto.class);
        chatMessage.setSender(user.getNickname());

        // 채팅 금지 확인
        if (adminChatService.isUserBanned(user.getNickname())) {
            sendToUser(user.getNickname(), Map.of("error", "채팅이 제한되어 메시지를 보낼 수 없습니다."));
            return;
        }

        // IP 추출
        String clientIp = (String) session.getAttributes().getOrDefault("clientIp", "unknown");
        
        // 저장 및 브로드캐스트
        try {
            chatService.save(chatMessage, clientIp);
            broadcast(chatMessage);
        } catch (Exception e) {
            log.error("❌ 채팅 저장 실패: {}", e.getMessage(), e);
            sendToUser(user.getNickname(), Map.of("error", "메시지 저장에 실패했습니다."));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Authentication auth = (Authentication) session.getAttributes().get("authentication");
        if (auth != null && auth.getPrincipal() instanceof User user) {
            sessions.remove(user.getNickname());
            log.debug("❌ WebSocket 연결 종료: {} (code: {})", user.getNickname(), status.getCode());
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
