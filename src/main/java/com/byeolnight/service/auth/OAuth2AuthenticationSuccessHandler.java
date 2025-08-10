package com.byeolnight.service.auth;

import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        try {
            String[] tokens = jwtTokenProvider.generateTokens(user.getId(), 
                    request.getHeader("User-Agent"), 
                    request.getRemoteAddr());
            
            String accessToken = tokens[0];
            String refreshToken = tokens[1];

            // 쿠키 설정
            response.addCookie(createCookie("accessToken", accessToken, 30 * 60)); // 30분
            response.addCookie(createCookie("refreshToken", refreshToken, 7 * 24 * 60 * 60)); // 7일

            String baseUrl = request.getServerName().contains("localhost") ? 
                    "http://localhost:5173" : "https://byeolnight.com";
            
            String redirectUrl;
            // 이메일 기반 닉네임으로 자동 생성되므로 바로 로그인 완료
            redirectUrl = UriComponentsBuilder.fromUriString(baseUrl + "/oauth/callback")
                    .queryParam("token", accessToken)
                    .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (Exception e) {
            log.error("OAuth2 인증 성공 처리 중 오류 발생", e);
            String baseUrl = request.getServerName().contains("localhost") ? 
                    "http://localhost:5173" : "https://byeolnight.com";
            String errorUrl = UriComponentsBuilder.fromUriString(baseUrl + "/oauth/callback")
                    .queryParam("error", "OAuth 로그인 처리 중 오류가 발생했습니다")
                    .build().toUriString();
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
        }
    }

    private jakarta.servlet.http.Cookie createCookie(String name, String value, int maxAge) {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // 로컬 개발용
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        return cookie;
    }
}