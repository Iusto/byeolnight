package com.byeolnight.config;

import com.byeolnight.entity.user.User;
import com.byeolnight.service.chat.AdminChatService;
import com.byeolnight.service.chat.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("채팅 WebSocket 핸들러 테스트")
class ChatWebSocketHandlerTest {

    @Test
    @DisplayName("닉네임이 복원된 인증 사용자 세션을 등록한다")
    void registersAuthenticatedUserWithNickname() {
        ChatWebSocketSessionRegistry registry = mock(ChatWebSocketSessionRegistry.class);
        ChatWebSocketHandler handler = handler(registry);
        User user = User.builder()
                .id(42L)
                .email("member@example.com")
                .nickname("별지기")
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .build();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-1");
        when(session.getAttributes()).thenReturn(Map.of(
                "authentication",
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())));

        handler.afterConnectionEstablished(session);

        verify(registry).register("별지기", session);
    }

    @Test
    @DisplayName("닉네임이 없는 불완전한 JWT 사용자는 세션 인덱스에 등록하지 않는다")
    void doesNotRegisterIncompleteJwtPrincipal() {
        ChatWebSocketSessionRegistry registry = mock(ChatWebSocketSessionRegistry.class);
        ChatWebSocketHandler handler = handler(registry);
        User incompleteUser = User.builder()
                .id(42L)
                .email("member@example.com")
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .build();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of(
                "authentication",
                new UsernamePasswordAuthenticationToken(incompleteUser, null, List.of())));

        handler.afterConnectionEstablished(session);

        verify(registry, never()).register(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private ChatWebSocketHandler handler(ChatWebSocketSessionRegistry registry) {
        return new ChatWebSocketHandler(
                mock(ChatService.class),
                mock(AdminChatService.class),
                new ObjectMapper(),
                registry);
    }
}
