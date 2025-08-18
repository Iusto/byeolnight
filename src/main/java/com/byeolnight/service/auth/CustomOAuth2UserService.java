package com.byeolnight.service.auth;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.service.certificate.CertificateService;
import com.byeolnight.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SocialAccountCleanupService socialAccountCleanupService;
    private final UserService userService;
    private final CertificateService certificateService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);
            OAuth2UserInfoFactory.OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User);
            
            validateEmail(userInfo.getEmail());
            
            User user = userRepository.findByEmail(userInfo.getEmail())
                    .map(existingUser -> processExistingUser(existingUser, userInfo, registrationId))
                    .orElseGet(() -> processNewUser(userInfo, registrationId));

            return new CustomOAuth2User(user, oAuth2User.getAttributes());
            
        } catch (OAuth2AuthenticationException e) {
            handleAuthenticationFailure(e, registrationId);
            throw e;
        }
    }
    
    private void validateEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException("이메일 정보를 가져올 수 없습니다.");
        }
    }
    
    private User processExistingUser(User existingUser, OAuth2UserInfoFactory.OAuth2UserInfo userInfo, String registrationId) {
        validateSocialAccount(existingUser, registrationId);
        validateAccountStatus(existingUser);
        return updateProfileImage(existingUser, userInfo.getImageUrl());
    }
    
    private void validateSocialAccount(User user, String registrationId) {
        if (!user.isSocialUser()) {
            String errorMsg = "해당 이메일로 이미 일반 계정이 존재합니다. 일반 로그인을 이용해주세요.";
            storeErrorMessage(errorMsg);
            throw new OAuth2AuthenticationException(errorMsg);
        }
        
        if (!registrationId.equals(user.getSocialProvider())) {
            String errorMsg = "해당 이메일은 다른 소셜 플랫폼(" + user.getSocialProviderName() + ")으로 가입되어 있습니다.";
            storeErrorMessage(errorMsg);
            throw new OAuth2AuthenticationException(errorMsg);
        }
    }
    
    private void validateAccountStatus(User user) {
        if (user.isAccountLocked()) {
            throwAccountError("계정이 잠겨있습니다. 관리자에게 문의하세요.");
        }
        
        switch (user.getStatus()) {
            case BANNED -> throwAccountError("계정이 밴되었습니다. 관리자에게 문의하세요.");
            case SUSPENDED -> throwAccountError("계정이 정지되었습니다. 관리자에게 문의하세요.");
            case WITHDRAWN -> throwAccountError("탈퇴한 계정입니다.");
        }
    }
    
    private void throwAccountError(String message) {
        storeErrorMessage(message);
        throw new OAuth2AuthenticationException(message);
    }
    
    private User processNewUser(OAuth2UserInfoFactory.OAuth2UserInfo userInfo, String registrationId) {
        if (socialAccountCleanupService.recoverWithdrawnAccount(userInfo.getEmail())) {
            log.info("30일 내 탈퇴한 소셜 계정 자동 복구: {}", userInfo.getEmail());
            return userRepository.findByEmail(userInfo.getEmail())
                    .map(recoveredUser -> recoverUser(recoveredUser, userInfo, registrationId))
                    .orElseGet(() -> createUser(userInfo, registrationId));
        }
        return createUser(userInfo, registrationId);
    }
    
    private User recoverUser(User recoveredUser, OAuth2UserInfoFactory.OAuth2UserInfo userInfo, String registrationId) {
        String baseNickname = userInfo.getEmail().split("@")[0];
        String uniqueNickname = generateUniqueNickname(baseNickname);
        recoveredUser.updateNickname(uniqueNickname, java.time.LocalDateTime.now());
        recoveredUser.setSocialProvider(registrationId);
        return updateProfileImage(recoveredUser, userInfo.getImageUrl());
    }
    
    private void handleAuthenticationFailure(OAuth2AuthenticationException e, String registrationId) {
        try {
            socialAccountCleanupService.handleFailedSocialLogin(null, registrationId);
        } catch (Exception cleanupError) {
            log.error("소셜 계정 정리 중 오류 발생", cleanupError);
        }
    }

    private User createUser(OAuth2UserInfoFactory.OAuth2UserInfo userInfo, String registrationId) {
        String nickname = generateUniqueNickname(userInfo.getEmail().split("@")[0]);
        
        User newUser = User.builder()
                .email(userInfo.getEmail())
                .password(null)
                .nickname(nickname)
                .profileImageUrl(userInfo.getImageUrl())
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .points(0)
                .nicknameChanged(false)
                .build();
        
        newUser.setSocialProvider(registrationId);
        User savedUser = userRepository.save(newUser);
        
        setupNewSocialUser(savedUser);
        return savedUser;
    }
    
    private void setupNewSocialUser(User user) {
        try {
            userService.grantDefaultAsteroidIcon(user);
            certificateService.checkAndIssueCertificates(user, CertificateService.CertificateCheckType.LOGIN);
            log.info("소셜 로그인 사용자 {}에게 기본 아이콘 및 인증서 발급 완료", user.getNickname());
        } catch (Exception e) {
            log.error("소셜 로그인 사용자 기본 설정 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    private User updateProfileImage(User user, String imageUrl) {
        if (imageUrl != null && !imageUrl.equals(user.getProfileImageUrl())) {
            User updatedUser = user.toBuilder().profileImageUrl(imageUrl).build();
            return userRepository.save(updatedUser);
        }
        return user;
    }

    private void storeErrorMessage(String errorMessage) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                request.getSession().setAttribute("oauth2_error_message", errorMessage);
                log.info("OAuth2 에러 메시지 세션에 저장: {}", errorMessage);
            }
        } catch (Exception e) {
            log.warn("OAuth2 에러 메시지 저장 실패: {}", e.getMessage());
        }
    }

    private String generateUniqueNickname(String baseNickname) {
        String normalizedNickname = normalizeNickname(baseNickname);
        
        // 기본 닉네임이 사용 가능하면 그대로 사용
        if (!userRepository.existsByNickname(normalizedNickname)) {
            return normalizedNickname;
        }
        
        // 중복된 경우 최대 10번 시도하여 고유한 닉네임 생성
        for (int attempt = 1; attempt <= 10; attempt++) {
            String uniqueSuffix = UUID.randomUUID().toString().substring(0, 4);
            String prefix = normalizedNickname.length() > 4 ? normalizedNickname.substring(0, 4) : normalizedNickname;
            String candidateNickname = prefix + uniqueSuffix;
            
            if (!userRepository.existsByNickname(candidateNickname)) {
                log.info("고유 닉네임 생성 완료: {} -> {} (시도 횟수: {})", baseNickname, candidateNickname, attempt);
                return candidateNickname;
            }
        }
        
        // 10번 시도해도 실패한 경우 타임스탬프 기반 닉네임 생성
        String timestampNickname = "사용자" + System.currentTimeMillis() % 100000;
        log.warn("닉네임 생성 최대 시도 초과, 타임스탬프 기반 닉네임 사용: {}", timestampNickname);
        return timestampNickname;
    }
    
    private String normalizeNickname(String nickname) {
        if (nickname.length() < 2) {
            return "사용자";
        }
        return nickname.length() > 8 ? nickname.substring(0, 8) : nickname;
    }
}