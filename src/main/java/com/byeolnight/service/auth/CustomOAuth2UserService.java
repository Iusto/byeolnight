package com.byeolnight.service.auth;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfoFactory.OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User);
        
        String email = userInfo.getEmail();
        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException("이메일 정보를 가져올 수 없습니다.");
        }

        User user = userRepository.findByEmail(email)
                .map(existingUser -> updateProfileImage(existingUser, userInfo.getImageUrl()))
                .orElseGet(() -> createUser(userInfo));

        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }

    private User createUser(OAuth2UserInfoFactory.OAuth2UserInfo userInfo) {
        String baseNickname = userInfo.getEmail().split("@")[0];
        String nickname = generateUniqueNickname(baseNickname);
        
        return userRepository.save(User.builder()
                .email(userInfo.getEmail())
                .nickname(nickname)
                .profileImageUrl(userInfo.getImageUrl())
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .emailVerified(true)
                .level(1)
                .points(0)
                .nicknameChanged(false)
                .build());
    }

    private User updateProfileImage(User user, String imageUrl) {
        if (imageUrl != null && !imageUrl.equals(user.getProfileImageUrl())) {
            User updatedUser = user.toBuilder().profileImageUrl(imageUrl).build();
            return userRepository.save(updatedUser);
        }
        return user;
    }

    private String generateUniqueNickname(String baseNickname) {
        if (!userRepository.existsByNickname(baseNickname)) {
            return baseNickname;
        }
        
        for (int i = 1; i <= 999; i++) {
            String nickname = baseNickname + i;
            if (!userRepository.existsByNickname(nickname)) {
                return nickname;
            }
        }
        
        return baseNickname + System.currentTimeMillis();
    }
}