package com.byeolnight.service.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/** 인증 컨트롤러들이 동일한 보안 속성으로 쿠키를 만들도록 한곳에서 관리한다. */
@Service
public class AuthCookieService {

    private final String cookieDomain;

    public AuthCookieService(@Value("${app.security.cookie.domain:}") String cookieDomain) {
        this.cookieDomain = cookieDomain;
    }

    public ResponseCookie createRefreshCookie(String token, long validityMillis, boolean rememberMe) {
        return applyDomain(ResponseCookie.from("refreshToken", token)
                .httpOnly(true).secure(true).sameSite("Lax").path("/")
                .maxAge(rememberMe ? validityMillis / 1000 : -1)).build();
    }

    public ResponseCookie createAccessCookie(String token, boolean rememberMe) {
        return applyDomain(ResponseCookie.from("accessToken", token)
                .httpOnly(true).secure(true).sameSite("Lax").path("/")
                .maxAge(rememberMe ? 1800 : -1)).build();
    }

    public ResponseCookie createDeleteCookie(String name) {
        return applyDomain(ResponseCookie.from(name, "")
                .httpOnly(true).secure(true).sameSite("Lax").path("/").maxAge(0)).build();
    }

    /** 브라우저가 전달한 영속 쿠키 흔적을 기준으로 로그인 유지 여부를 보존한다. */
    public boolean isRememberMeCookie(String cookieHeader) {
        return cookieHeader != null && cookieHeader.contains("refreshToken")
                && (cookieHeader.contains("Max-Age") || cookieHeader.contains("Expires"));
    }

    private ResponseCookie.ResponseCookieBuilder applyDomain(ResponseCookie.ResponseCookieBuilder builder) {
        return cookieDomain == null || cookieDomain.isBlank() ? builder : builder.domain(cookieDomain);
    }
}
