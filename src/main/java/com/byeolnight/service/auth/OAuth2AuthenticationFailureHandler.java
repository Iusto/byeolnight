package com.byeolnight.service.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final String ERROR_SESSION_KEY = "oauth2_error_message";
    private static final String DEFAULT_ERROR_MESSAGE = "OAuth 로그인에 실패했습니다";
    private static final String CALLBACK_PATH = "/oauth/callback";
    private static final String LOCAL_BASE_URL = "http://localhost:5173";
    private static final String PROD_BASE_URL = "https://byeolnight.com";

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        
        log.error("OAuth2 인증 실패 - 예외 타입: {}, 메시지: '{}'", 
                exception.getClass().getSimpleName(), exception.getMessage());
        
        String errorMessage = extractErrorMessage(request, exception);
        String redirectUrl = buildRedirectUrl(request, errorMessage);

        log.info("OAuth2 실패 리다이렉트 - 원본 메시지: '{}', URL: {}", errorMessage, redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
    
    private String extractErrorMessage(HttpServletRequest request, AuthenticationException exception) {
        String errorMessage = (String) request.getSession().getAttribute(ERROR_SESSION_KEY);
        if (errorMessage != null) {
            request.getSession().removeAttribute(ERROR_SESSION_KEY);
            return errorMessage;
        }
        
        String exceptionMessage = exception.getMessage();
        return (exceptionMessage != null && !exceptionMessage.isEmpty()) ? 
                exceptionMessage : DEFAULT_ERROR_MESSAGE;
    }
    
    private String buildRedirectUrl(HttpServletRequest request, String errorMessage) {
        String baseUrl = isLocalhost(request) ? LOCAL_BASE_URL : PROD_BASE_URL;
        
        // 복구 가능한 계정인 경우 복구 페이지로 리다이렉트
        if (errorMessage.startsWith("RECOVERABLE_ACCOUNT:")) {
            String[] parts = errorMessage.split(":");
            if (parts.length >= 3) {
                String email = parts[1];
                String provider = parts[2];
                return UriComponentsBuilder.fromUriString(baseUrl + "/oauth/recover")
                        .queryParam("email", URLEncoder.encode(email, StandardCharsets.UTF_8))
                        .queryParam("provider", provider)
                        .build().toUriString();
            }
        }
        
        String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        return UriComponentsBuilder.fromUriString(baseUrl + CALLBACK_PATH)
                .queryParam("error", encodedError)
                .build().toUriString();
    }
    
    private boolean isLocalhost(HttpServletRequest request) {
        return request.getServerName().contains("localhost");
    }
}