package com.byeolnight.service.auth;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, OAuth2User oAuth2User) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> new GoogleOAuth2UserInfo(oAuth2User.getAttributes());
            case "kakao" -> new KakaoOAuth2UserInfo(oAuth2User.getAttributes());
            case "naver" -> new NaverOAuth2UserInfo(oAuth2User.getAttributes());
            default -> throw new IllegalArgumentException("지원하지 않는 OAuth2 제공자입니다: " + registrationId);
        };
    }

    public interface OAuth2UserInfo {
        String getEmail();
        String getName();
        String getImageUrl();
    }

    public static class GoogleOAuth2UserInfo implements OAuth2UserInfo {
        private final Map<String, Object> attributes;

        public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        @Override
        public String getEmail() {
            return (String) attributes.get("email");
        }

        @Override
        public String getName() {
            return (String) attributes.get("name");
        }

        @Override
        public String getImageUrl() {
            return (String) attributes.get("picture");
        }
    }

    public static class KakaoOAuth2UserInfo implements OAuth2UserInfo {
        private final Map<String, Object> attributes;

        public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        @Override
        public String getEmail() {
            Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
            return account != null ? (String) account.get("email") : null;
        }

        @Override
        public String getName() {
            Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
            return properties != null ? (String) properties.get("nickname") : null;
        }

        @Override
        public String getImageUrl() {
            Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
            return properties != null ? (String) properties.get("profile_image") : null;
        }
    }

    public static class NaverOAuth2UserInfo implements OAuth2UserInfo {
        private final Map<String, Object> attributes;

        public NaverOAuth2UserInfo(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        @Override
        public String getEmail() {
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            return response != null ? (String) response.get("email") : null;
        }

        @Override
        public String getName() {
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            return response != null ? (String) response.get("name") : null;
        }

        @Override
        public String getImageUrl() {
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            return response != null ? (String) response.get("profile_image") : null;
        }
    }
}