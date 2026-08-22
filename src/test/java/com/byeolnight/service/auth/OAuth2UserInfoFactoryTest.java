package com.byeolnight.service.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OAuth 제공자 사용자 정보 변환")
class OAuth2UserInfoFactoryTest {

    @Test
    @DisplayName("Google의 sub와 이메일 검증 상태를 읽는다")
    void mapsGoogleSubjectAndVerifiedEmail() {
        var principal = new DefaultOAuth2User(
                List.of(),
                Map.of("sub", "google-sub", "email", "user@example.com", "email_verified", true),
                "sub"
        );

        var result = OAuth2UserInfoFactory.getOAuth2UserInfo("google", principal);

        assertThat(result.getProviderUserId()).isEqualTo("google-sub");
        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("Kakao와 Naver의 중첩 사용자 ID를 읽는다")
    void mapsNestedProviderIds() {
        var kakao = new DefaultOAuth2User(
                List.of(),
                Map.of("id", 1234L, "kakao_account", Map.of("email", "kakao@example.com")),
                "id"
        );
        var naver = new DefaultOAuth2User(
                List.of(),
                Map.of("response", Map.of("id", "naver-id", "email", "naver@example.com")),
                "response"
        );

        assertThat(OAuth2UserInfoFactory.getOAuth2UserInfo("kakao", kakao).getProviderUserId())
                .isEqualTo("1234");
        assertThat(OAuth2UserInfoFactory.getOAuth2UserInfo("naver", naver).getProviderUserId())
                .isEqualTo("naver-id");
    }
}
