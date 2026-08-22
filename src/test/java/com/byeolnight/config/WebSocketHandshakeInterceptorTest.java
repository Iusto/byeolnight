package com.byeolnight.config;

import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.security.JwtTokenProvider;
import com.byeolnight.service.auth.TokenService;
import com.byeolnight.service.user.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("WebSocket 인증 인터셉터 테스트")
class WebSocketHandshakeInterceptorTest {

    @Test
    @DisplayName("JWT 사용자 ID로 닉네임을 포함한 전체 사용자를 복원한다")
    void restoresFullUserForAuthenticatedChat() throws Exception {
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        TokenService tokenService = mock(TokenService.class);
        WebSocketHandshakeInterceptor interceptor = new WebSocketHandshakeInterceptor(
                tokenProvider, userDetailsService, tokenService);

        User fullUser = User.builder()
                .id(42L)
                .email("member@example.com")
                .nickname("별지기")
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .build();
        given(tokenProvider.validate("access-token")).willReturn(true);
        given(tokenProvider.getUserIdFromToken("access-token")).willReturn(42L);
        given(tokenService.isAccessTokenBlacklisted("access-token")).willReturn(false);
        given(userDetailsService.loadUserByUsername("42")).willReturn(fullUser);

        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "accessToken=access-token; refreshToken=refresh-token");
        given(request.getHeaders()).willReturn(headers);
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = interceptor.beforeHandshake(
                request,
                mock(ServerHttpResponse.class),
                mock(WebSocketHandler.class),
                attributes);

        assertThat(accepted).isTrue();
        Authentication authentication = (Authentication) attributes.get("authentication");
        assertThat(authentication.getPrincipal()).isSameAs(fullUser);
        assertThat(((User) authentication.getPrincipal()).getNickname()).isEqualTo("별지기");
    }
}
