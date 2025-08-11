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

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SocialAccountCleanupService socialAccountCleanupService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);
            OAuth2UserInfoFactory.OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User);
            
            String email = userInfo.getEmail();
            if (email == null || email.isEmpty()) {
                throw new OAuth2AuthenticationException("이메일 정보를 가져올 수 없습니다.");
            }

            User user = userRepository.findByEmail(email)
                    .map(existingUser -> {
                        // 일시정지된 소셜 계정 복구
                        if (existingUser.isSocialUser() && existingUser.getStatus() == User.UserStatus.SUSPENDED) {
                            existingUser.changeStatus(User.UserStatus.ACTIVE);
                            log.info("일시정지된 소셜 계정 복구: {}", email);
                        }
                        return updateProfileImage(existingUser, userInfo.getImageUrl());
                    })
                    .orElseGet(() -> {
                        // 30일 내 탈퇴한 소셜 계정 자동 복구 시도
                        boolean recovered = socialAccountCleanupService.recoverWithdrawnAccount(email);
                        if (recovered) {
                            log.info("30일 내 탈퇴한 소셜 계정 자동 복구: {}", email);
                            // 복구된 계정 다시 조회
                            return userRepository.findByEmail(email)
                                    .map(recoveredUser -> updateProfileImage(recoveredUser, userInfo.getImageUrl()))
                                    .orElseGet(() -> createUser(userInfo, registrationId));
                        }
                        return createUser(userInfo, registrationId);
                    });

            return new CustomOAuth2User(user, oAuth2User.getAttributes());
            
        } catch (OAuth2AuthenticationException e) {
            // 소셜 로그인 실패 시 계정 정리 서비스 호출
            try {
                String email = extractEmailFromError(e, userRequest);
                if (email != null) {
                    socialAccountCleanupService.handleFailedSocialLogin(email, registrationId);
                }
            } catch (Exception cleanupError) {
                log.error("소셜 계정 정리 중 오류 발생", cleanupError);
            }
            throw e;
        }
    }
    
    private String extractEmailFromError(OAuth2AuthenticationException e, OAuth2UserRequest userRequest) {
        // 에러에서 이메일 추출 시도 (제한적)
        try {
            // 실제로는 복잡한 로직이 필요하지만, 여기서는 간단히 처리
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private User createUser(OAuth2UserInfoFactory.OAuth2UserInfo userInfo, String registrationId) {
        String baseNickname = userInfo.getEmail().split("@")[0];
        String nickname = generateUniqueNickname(baseNickname);
        
        User newUser = User.builder()
                .email(userInfo.getEmail())
                .password(null) // 소셜 로그인 사용자는 비밀번호 없음
                .nickname(nickname)
                .profileImageUrl(userInfo.getImageUrl())
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)

                .points(0)
                .nicknameChanged(false)
                .build();
        
        newUser.setSocialProvider(registrationId);
        User savedUser = userRepository.save(newUser);
        
        // 소셜 로그인 사용자에게 기본 아이콘 부여 및 인증서 발급
        try {
            // UserService를 통해 기본 아이콘 부여
            com.byeolnight.service.user.UserService userService = 
                com.byeolnight.infrastructure.config.ApplicationContextProvider.getBean(com.byeolnight.service.user.UserService.class);
            userService.grantDefaultAsteroidIcon(savedUser);
            
            // CertificateService를 통해 별빛탐험가 인증서 발급
            com.byeolnight.service.certificate.CertificateService certificateService = 
                com.byeolnight.infrastructure.config.ApplicationContextProvider.getBean(com.byeolnight.service.certificate.CertificateService.class);
            certificateService.checkAndIssueCertificates(savedUser, com.byeolnight.service.certificate.CertificateService.CertificateCheckType.LOGIN);
            
            log.info("소셜 로그인 사용자 {}에게 기본 아이콘 및 인증서 발급 완료", savedUser.getNickname());
        } catch (Exception e) {
            log.error("소셜 로그인 사용자 기본 설정 중 오류 발생: {}", e.getMessage(), e);
        }
        
        return savedUser;
    }

    private User updateProfileImage(User user, String imageUrl) {
        if (imageUrl != null && !imageUrl.equals(user.getProfileImageUrl())) {
            User updatedUser = user.toBuilder().profileImageUrl(imageUrl).build();
            return userRepository.save(updatedUser);
        }
        return user;
    }

    private String generateUniqueNickname(String baseNickname) {
        // 기본 닉네임이 너무 짧으면 보완
        if (baseNickname.length() < 2) {
            baseNickname = "새로운사용자";
        }
        
        // 기본 닉네임 시도 (1번만)
        if (!userRepository.existsByNickname(baseNickname)) {
            return baseNickname;
        }
        
        // UUID 기반 유니크 닉네임 생성 (DB 조회 최대 2번)
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        return baseNickname + "_" + uniqueSuffix;
    }
}