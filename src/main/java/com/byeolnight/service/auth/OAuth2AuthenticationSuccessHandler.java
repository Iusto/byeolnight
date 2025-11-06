package com.byeolnight.service.auth;

import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import com.byeolnight.service.auth.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    
    @Value("${app.frontend.local-url:http://localhost:5173}")
    private String localFrontendUrl;
    
    @Value("${app.frontend.prod-url:https://byeolnight.com}")
    private String prodFrontendUrl;
    
    @Value("${app.security.cookie.access-token-max-age:1800}")
    private int accessTokenMaxAge;
    
    @Value("${app.security.cookie.domain:}")
    private String cookieDomain;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        try {
            String accessToken = jwtTokenProvider.createAccessToken(user);
            String refreshToken = jwtTokenProvider.createRefreshToken(user);
            long refreshTokenValidity = jwtTokenProvider.getRefreshTokenValidity();
            
            // Redis에 Refresh Token 저장
            tokenService.saveRefreshToken(user.getEmail(), refreshToken, refreshTokenValidity);

            // ResponseCookie로 HttpOnly 쿠키 설정 (일반 로그인과 동일)
            ResponseCookie refreshCookie = createRefreshCookie(refreshToken, refreshTokenValidity);
            ResponseCookie accessCookie = createAccessCookie(accessToken);
            
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
            
            // OAuth2 인증 완료 후 세션 정리
            HttpSession session = request.getSession(false);
            if (session != null) {
                // 복구 체크 건너뛰기 플래그 제거 (이메일별)
                session.removeAttribute("skip_recovery_check_" + user.getEmail());
                session.invalidate();
                log.info("OAuth2 로그인 완료 후 세션 무효화: {}", user.getEmail());
            }

            ResponseCookie deleteJSessionId = ResponseCookie.from("JSESSIONID", "")
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(0)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, deleteJSessionId.toString());

            String baseUrl = getBaseUrl(request);
            String redirectUrl = baseUrl + "/oauth/callback";

            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (Exception e) {
            log.error("OAuth2 인증 성공 처리 중 오류 발생", e);
            String baseUrl = getBaseUrl(request);
            String errorUrl = UriComponentsBuilder.fromUriString(baseUrl + "/oauth/callback")
                    .queryParam("error", "OAuth 로그인 처리 중 오류가 발생했습니다")
                    .build().toUriString();
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
        }
    }

    private String getBaseUrl(HttpServletRequest request) {
        return request.getServerName().contains("localhost") ? localFrontendUrl : prodFrontendUrl;
    }

    private ResponseCookie createRefreshCookie(String refreshToken, long validity) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(-1);
        
        if (!cookieDomain.isEmpty()) {
            builder.domain(cookieDomain);
        }
        
        return builder.build();
    }

    private ResponseCookie createAccessCookie(String accessToken) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(-1);
        
        if (!cookieDomain.isEmpty()) {
            builder.domain(cookieDomain);
        }
        
        return builder.build();
    }
}