package com.byeolnight.config;

import com.byeolnight.infrastructure.config.WebCorsProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketConfigTest {

    @Test
    @DisplayName("WebSocket도 HTTP와 동일한 신뢰 Origin만 허용한다")
    void usesSharedTrustedOrigins() {
        ChatWebSocketHandler handler = mock(ChatWebSocketHandler.class);
        WebSocketHandshakeInterceptor interceptor = mock(WebSocketHandshakeInterceptor.class);
        WebCorsProperties properties = new WebCorsProperties();
        properties.setAllowedOrigins(List.of("http://localhost:5173", "https://byeolnight.com"));
        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration registration = mock(WebSocketHandlerRegistration.class);

        when(registry.addHandler(handler, "/ws")).thenReturn(registration);
        when(registration.setAllowedOrigins("http://localhost:5173", "https://byeolnight.com"))
                .thenReturn(registration);

        new WebSocketConfig(handler, interceptor, properties).registerWebSocketHandlers(registry);

        verify(registration).setAllowedOrigins("http://localhost:5173", "https://byeolnight.com");
        verify(registration).addInterceptors(interceptor);
    }
}
