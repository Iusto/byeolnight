package com.byeolnight.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("ChatWebSocketSessionRegistry 테스트")
class ChatWebSocketSessionRegistryTest {

    private ChatWebSocketSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ChatWebSocketSessionRegistry();
    }

    @Test
    @DisplayName("같은 사용자의 여러 탭에 메시지를 모두 전송")
    void shouldBroadcastToMultipleSessionsOfSameUser() throws IOException {
        // given
        WebSocketSession firstSession = openSession("session-1");
        WebSocketSession secondSession = openSession("session-2");
        registry.register("별지기", firstSession);
        registry.register("별지기", secondSession);

        // when
        registry.broadcast(new TextMessage("메시지"));

        // then
        verify(firstSession).sendMessage(any(TextMessage.class));
        verify(secondSession).sendMessage(any(TextMessage.class));
        assertThat(registry.activeSessionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("한 탭 종료가 같은 사용자의 다른 탭 연결을 제거하지 않음")
    void shouldRemoveOnlyClosedSession() throws IOException {
        // given
        WebSocketSession firstSession = openSession("session-1");
        WebSocketSession secondSession = openSession("session-2");
        registry.register("별지기", firstSession);
        registry.register("별지기", secondSession);

        // when
        registry.unregister("별지기", firstSession);
        registry.sendToUser("별지기", new TextMessage("개인 알림"));

        // then
        verify(firstSession, times(0)).sendMessage(any(TextMessage.class));
        verify(secondSession).sendMessage(any(TextMessage.class));
        assertThat(registry.activeSessionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("등록되지 않은 비로그인 세션도 pong을 직접 전송")
    void shouldSendPongToAnonymousSession() throws IOException {
        // given
        WebSocketSession anonymousSession = openSession("anonymous-session");

        // when
        registry.sendToSession(anonymousSession, new TextMessage("{\"type\":\"pong\"}"));

        // then
        verify(anonymousSession).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("동시에 발생한 브로드캐스트를 한 세션에 순차 전송")
    void shouldSerializeConcurrentBroadcasts() throws Exception {
        // given
        WebSocketSession session = openSession("session-1");
        CountDownLatch firstSendStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstSend = new CountDownLatch(1);
        AtomicInteger activeSends = new AtomicInteger();
        AtomicInteger maximumConcurrentSends = new AtomicInteger();

        doAnswer(invocation -> {
            int active = activeSends.incrementAndGet();
            maximumConcurrentSends.accumulateAndGet(active, Math::max);
            firstSendStarted.countDown();
            releaseFirstSend.await(1, TimeUnit.SECONDS);
            activeSends.decrementAndGet();
            return null;
        }).when(session).sendMessage(any(TextMessage.class));

        registry.register("별지기", session);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // when
            Future<?> first = executor.submit(() -> registry.broadcast(new TextMessage("첫 메시지")));
            assertThat(firstSendStarted.await(1, TimeUnit.SECONDS)).isTrue();
            Future<?> second = executor.submit(() -> registry.broadcast(new TextMessage("두 번째 메시지")));
            releaseFirstSend.countDown();
            first.get(1, TimeUnit.SECONDS);
            second.get(1, TimeUnit.SECONDS);

            // then
            verify(session, times(2)).sendMessage(any(TextMessage.class));
            assertThat(maximumConcurrentSends).hasValue(1);
        } finally {
            releaseFirstSend.countDown();
            executor.shutdownNow();
        }
    }

    private WebSocketSession openSession(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.getId()).willReturn(id);
        given(session.isOpen()).willReturn(true);
        return session;
    }
}
