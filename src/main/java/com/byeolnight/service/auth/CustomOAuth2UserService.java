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
                .orElseGet(() -> createUser(userInfo, registrationId));

        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }

    private User createUser(OAuth2UserInfoFactory.OAuth2UserInfo userInfo, String provider) {
        // OAuth2 사용자는 임시 닉네임으로 생성 (나중에 설정 필요)
        String tempNickname = generateTempNickname();
        
        User user = User.builder()
                .email(userInfo.getEmail())
                .nickname(tempNickname)
                .profileImageUrl(userInfo.getImageUrl())
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .emailVerified(true)
                .level(1)
                .points(0)
                .nicknameChanged(false) // 닉네임 설정이 필요함을 표시
                .build();

        return userRepository.save(user);
    }

    private String generateTempNickname() {
        String baseNickname = "임시사용자";
        String nickname = baseNickname + System.currentTimeMillis();
        
        // 중복 체크 (매우 낮은 확률이지만)
        while (userRepository.existsByNickname(nickname)) {
            nickname = baseNickname + System.currentTimeMillis() + (int)(Math.random() * 1000);
        }
        
        return nickname;
    }
}