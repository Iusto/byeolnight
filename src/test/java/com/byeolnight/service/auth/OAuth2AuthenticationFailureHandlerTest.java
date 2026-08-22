package com.byeolnight.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OAuth 인증 실패 리다이렉트")
class OAuth2AuthenticationFailureHandlerTest {

    private OAuth2AuthenticationFailureHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2AuthenticationFailureHandler();
        ReflectionTestUtils.setField(handler, "localFrontendUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(handler, "prodFrontendUrl", "https://byeolnight.com");
    }

    @Test
    @DisplayName("복구 화면에는 이메일 대신 일회용 티켓만 전달한다")
    void recoveryRedirectContainsOnlyTicket() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("localhost");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                request,
                response,
                new OAuth2RecoveryRequiredException("one-time-ticket")
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/oauth/recover?ticket=one-time-ticket")
                .doesNotContain("email=")
                .doesNotContain("provider=");
    }

    @Test
    @DisplayName("일반 실패는 내부 예외 메시지 대신 제한된 오류 코드만 전달한다")
    void genericFailureRedirectUsesPublicErrorCode() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("byeolnight.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                request,
                response,
                new org.springframework.security.authentication.BadCredentialsException("민감한 내부 오류")
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://byeolnight.com/oauth/callback?error=OAUTH_LOGIN_FAILED")
                .doesNotContain("민감한 내부 오류");
    }
}
