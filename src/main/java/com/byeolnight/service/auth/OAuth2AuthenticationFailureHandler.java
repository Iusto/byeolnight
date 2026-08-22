package com.byeolnight.service.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final String CALLBACK_PATH = "/oauth/callback";

    @Value("${app.frontend.local-url:http://localhost:5173}")
    private String localFrontendUrl;

    @Value("${app.frontend.prod-url:https://byeolnight.com}")
    private String prodFrontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        OAuth2RecoveryRequiredException recoveryException = findRecoveryException(exception);
        String redirectUrl;

        if (recoveryException != null) {
            redirectUrl = UriComponentsBuilder
                    .fromUriString(getBaseUrl(request) + "/oauth/recover")
                    .queryParam("ticket", recoveryException.getRecoveryTicket())
                    .build()
                    .toUriString();
            log.info("OAuth2 탈퇴 계정 복구 확인 요청");
        } else {
            redirectUrl = UriComponentsBuilder
                    .fromUriString(getBaseUrl(request) + CALLBACK_PATH)
                    .queryParam("error", "OAUTH_LOGIN_FAILED")
                    .build()
                    .toUriString();
            log.warn("OAuth2 인증 실패: 예외유형={}", exception.getClass().getSimpleName());
        }

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private OAuth2RecoveryRequiredException findRecoveryException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof OAuth2RecoveryRequiredException recoveryException) {
                return recoveryException;
            }
            current = current.getCause();
        }
        return null;
    }

    private String getBaseUrl(HttpServletRequest request) {
        return request.getServerName().contains("localhost") ? localFrontendUrl : prodFrontendUrl;
    }
}
