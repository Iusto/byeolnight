package com.byeolnight.config.security;

import com.byeolnight.entity.user.User;
import com.byeolnight.service.auth.AuthService;
import com.byeolnight.infrastructure.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        
        try {
            String registrationId = request.getParameter("registration_id");
            if (registrationId == null) {
                registrationId = "google";
            }
            
            User user = authService.findOrCreateOAuthUser(registrationId, oAuth2User);
            
            if (authService.needsNicknameSetup(user.getId())) {
                getRedirectStrategy().sendRedirect(request, response, 
                    "http://localhost:5173/auth/setup-nickname?userId=" + user.getId());
                return;
            }
            
            String[] tokens = authService.loginUser(user, getClientInfo(request), getClientIP(request), response);
            
            String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/auth/oauth-success")
                .queryParam("accessToken", tokens[0])
                .build().toUriString();
                
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            
        } catch (Exception e) {
            log.error("OAuth2 login processing error", e);
            getRedirectStrategy().sendRedirect(request, response, "http://localhost:5173/auth/login?error=oauth_error");
        }
    }

    private String getClientInfo(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}