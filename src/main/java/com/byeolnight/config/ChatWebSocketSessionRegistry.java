package com.byeolnight.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인증된 채팅 WebSocket 세션과 안전한 메시지 전송을 관리한다.
 *
 * <p>Spring WebSocket 세션은 여러 스레드가 동시에 {@code sendMessage}를 호출하는 것을
 * 보장하지 않는다. 채팅 브로드캐스트와 pong/제재 알림이 겹치면 연결이 종료될 수 있으므로
 * {@link ConcurrentWebSocketSessionDecorator}로 전송을 직렬화한다. 또한 닉네임 하나에
 * 세션 하나만 저장하던 구조를 개선해 여러 탭이나 기기 연결을 각각 유지한다.</p>
 */
@Slf4j
@Component
public class ChatWebSocketSessionRegistry {

    private static final int SEND_TIME_LIMIT_MILLIS = 10_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 64 * 1024;

    private final Map<String, ConcurrentWebSocketSessionDecorator> sessionsById = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionIdsByNickname = new ConcurrentHashMap<>();

    public void register(String nickname, WebSocketSession session) {
        ConcurrentWebSocketSessionDecorator safeSession = new ConcurrentWebSocketSessionDecorator(
                session, SEND_TIME_LIMIT_MILLIS, BUFFER_SIZE_LIMIT_BYTES);
        sessionsById.put(session.getId(), safeSession);
        sessionIdsByNickname.computeIfAbsent(nickname, ignored -> ConcurrentHashMap.newKeySet())
                .add(session.getId());
    }

    public void unregister(String nickname, WebSocketSession session) {
        sessionsById.remove(session.getId());
        sessionIdsByNickname.computeIfPresent(nickname, (ignored, sessionIds) -> {
            sessionIds.remove(session.getId());
            return sessionIds.isEmpty() ? null : sessionIds;
        });
    }

    public void sendToSession(WebSocketSession session, TextMessage message) throws IOException {
        WebSocketSession target = sessionsById.get(session.getId());
        if (target == null) {
            target = session;
        }
        target.sendMessage(message);
    }

    public void sendToUser(String nickname, TextMessage message) {
        Set<String> sessionIds = sessionIdsByNickname.getOrDefault(nickname, Set.of());
        sessionIds.forEach(sessionId -> sendSafely(sessionId, message));
    }

    public void broadcast(TextMessage message) {
        sessionsById.keySet().forEach(sessionId -> sendSafely(sessionId, message));
    }

    int activeSessionCount() {
        return sessionsById.size();
    }

    private void sendSafely(String sessionId, TextMessage message) {
        WebSocketSession session = sessionsById.get(sessionId);
        if (session == null) return;

        if (!session.isOpen()) {
            sessionsById.remove(sessionId);
            removeSessionIdFromNicknameIndex(sessionId);
            return;
        }

        try {
            session.sendMessage(message);
        } catch (Exception e) {
            log.warn("WebSocket 메시지 전송 실패: sessionId={}, error={}", sessionId, e.getMessage());
        }
    }

    private void removeSessionIdFromNicknameIndex(String sessionId) {
        sessionIdsByNickname.forEach((nickname, sessionIds) -> {
            if (sessionIds.remove(sessionId) && sessionIds.isEmpty()) {
                sessionIdsByNickname.remove(nickname, sessionIds);
            }
        });
    }
}
