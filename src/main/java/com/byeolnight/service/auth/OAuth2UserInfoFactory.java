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
            return getNestedString(attributes, "kakao_account", "email");
        }

        @Override
        public String getName() {
            return getNestedString(attributes, "properties", "nickname");
        }

        @Override
        public String getImageUrl() {
            return getNestedString(attributes, "properties", "profile_image");
        }
    }

    public static class NaverOAuth2UserInfo implements OAuth2UserInfo {
        private final Map<String, Object> attributes;

        public NaverOAuth2UserInfo(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        @Override
        public String getEmail() {
            return getNestedString(attributes, "response", "email");
        }

        @Override
        public String getName() {
            return getNestedString(attributes, "response", "name");
        }

        @Override
        public String getImageUrl() {
            return getNestedString(attributes, "response", "profile_image");
        }
    }

    @SuppressWarnings("unchecked")
    private static String getNestedString(Map<String, Object> attributes, String outerKey, String innerKey) {
        Object nested = attributes.get(outerKey);
        if (nested instanceof Map<?, ?> nestedMap) {
            Object value = ((Map<String, Object>) nestedMap).get(innerKey);
            return value instanceof String s ? s : null;
        }
        return null;
    }
}