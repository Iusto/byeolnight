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
        String getProviderUserId();
        String getEmail();
        String getName();
        String getImageUrl();
        Boolean isEmailVerified();
    }

    public static class GoogleOAuth2UserInfo implements OAuth2UserInfo {
        private final Map<String, Object> attributes;

        public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        @Override
        public String getProviderUserId() {
            return (String) attributes.get("sub");
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

        @Override
        public Boolean isEmailVerified() {
            Object value = attributes.get("email_verified");
            return value instanceof Boolean verified ? verified : null;
        }
    }

    public static class KakaoOAuth2UserInfo implements OAuth2UserInfo {
        private final Map<String, Object> attributes;

        public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        @Override
        public String getProviderUserId() {
            Object value = attributes.get("id");
            return value == null ? null : value.toString();
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

        @Override
        public Boolean isEmailVerified() {
            Object account = attributes.get("kakao_account");
            if (account instanceof Map<?, ?> accountMap) {
                Object valid = accountMap.get("is_email_valid");
                Object verified = accountMap.get("is_email_verified");
                if (valid instanceof Boolean validEmail && !validEmail) {
                    return false;
                }
                return verified instanceof Boolean verifiedEmail ? verifiedEmail : null;
            }
            return null;
        }
    }

    public static class NaverOAuth2UserInfo implements OAuth2UserInfo {
        private final Map<String, Object> attributes;

        public NaverOAuth2UserInfo(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        @Override
        public String getProviderUserId() {
            return getNestedString(attributes, "response", "id");
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

        @Override
        public Boolean isEmailVerified() {
            return null;
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
