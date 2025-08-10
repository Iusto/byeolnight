package com.byeolnight.service.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        
        log.error("OAuth2 인증 실패: {}", exception.getMessage());
        
        String errorMessage = "OAuth 로그인에 실패했습니다";
        if (exception.getMessage() != null) {
            errorMessage = exception.getMessage();
        }

        String baseUrl = request.getServerName().contains("localhost") ? 
                "http://localhost:5173" : "https://byeolnight.com";
        String redirectUrl = UriComponentsBuilder.fromUriString(baseUrl + "/oauth/callback")
                .queryParam("error", errorMessage)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}