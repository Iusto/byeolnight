package com.byeolnight.service.auth;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 검증된 OAuth 제공자 정보로 애플리케이션 회원을 판별한다.
 *
 * <p>불변 식별자인 제공자 회원 ID를 우선 사용하고, 기존 데이터에 제공자 ID가
 * 없는 경우에만 이메일 조회를 허용한다. 이메일은 변경되거나 재사용될 수 있으므로
 * 신규 데이터의 주 식별자로 사용하지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
public class OAuthAccountService {

    private final UserRepository userRepository;
    private final SocialUserRegistrationService socialUserRegistrationService;
    private final AccountRecoveryService accountRecoveryService;
    private final AccountRecoveryTicketService accountRecoveryTicketService;

    /** OAuth 제공자 정보와 연결된 기존 회원을 인증하거나 신규 회원을 생성한다. */
    @Transactional
    public User authenticate(String provider, OAuth2UserInfoFactory.OAuth2UserInfo userInfo) {
        validateUserInfo(userInfo);

        return userRepository
                .findBySocialProviderAndSocialProviderId(provider, userInfo.getProviderUserId())
                .map(user -> authenticateExistingUser(user, provider, userInfo))
                // provider ID 도입 전에 가입한 레거시 회원만 이메일 경로로 연결한다.
                .orElseGet(() -> userRepository.findByEmail(userInfo.getEmail())
                        .map(user -> authenticateExistingUser(user, provider, userInfo))
                        .orElseGet(() -> socialUserRegistrationService.register(provider, userInfo)));
    }

    private User authenticateExistingUser(User user, String provider,
                                          OAuth2UserInfoFactory.OAuth2UserInfo userInfo) {
        validateSocialAccount(user, provider);
        validateAndLinkSocialIdentity(user, provider, userInfo.getProviderUserId());
        validateAccountStatus(user, provider, userInfo.getProviderUserId());
        return updateProfileImage(user, userInfo.getImageUrl());
    }

    private void validateUserInfo(OAuth2UserInfoFactory.OAuth2UserInfo userInfo) {
        if (userInfo.getProviderUserId() == null || userInfo.getProviderUserId().isBlank()) {
            throw new OAuth2AuthenticationException("외부 계정 식별 정보를 가져올 수 없습니다.");
        }
        if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            throw new OAuth2AuthenticationException("이메일 정보를 가져올 수 없습니다.");
        }
        if (Boolean.FALSE.equals(userInfo.isEmailVerified())) {
            throw new OAuth2AuthenticationException("인증되지 않은 이메일은 사용할 수 없습니다.");
        }
    }

    private void validateSocialAccount(User user, String provider) {
        if (!user.isSocialUser()) {
            throw new OAuth2AuthenticationException(
                    "해당 이메일로 이미 일반 계정이 존재합니다. 일반 로그인을 이용해주세요.");
        }
        if (!provider.equals(user.getSocialProvider())) {
            throw new OAuth2AuthenticationException(
                    "해당 이메일은 다른 소셜 플랫폼(" + user.getSocialProviderName() + ")으로 가입되어 있습니다.");
        }
    }

    private void validateAndLinkSocialIdentity(User user, String provider, String providerUserId) {
        if (user.getSocialProviderId() == null) {
            // 이메일로만 저장된 레거시 회원은 재인증에 성공한 현재 제공자 ID를 한 번 연결한다.
            user.linkSocialIdentity(provider, providerUserId);
            return;
        }
        if (!providerUserId.equals(user.getSocialProviderId())) {
            throw new OAuth2AuthenticationException("외부 계정 식별 정보가 일치하지 않습니다.");
        }
    }

    private void validateAccountStatus(User user, String provider, String providerUserId) {
        if (user.isAccountLocked()) {
            throw new OAuth2AuthenticationException("계정이 잠겨있습니다. 관리자에게 문의하세요.");
        }

        switch (user.getStatus()) {
            case BANNED -> throw new OAuth2AuthenticationException("계정이 밴되었습니다. 관리자에게 문의하세요.");
            case SUSPENDED -> throw new OAuth2AuthenticationException("계정이 정지되었습니다. 관리자에게 문의하세요.");
            case WITHDRAWN -> issueRecoveryTicketOrReject(user, provider, providerUserId);
            case ACTIVE -> {
                // 활성 계정은 추가 상태 처리 없이 로그인을 계속한다.
            }
        }
    }

    private void issueRecoveryTicketOrReject(User user, String provider, String providerUserId) {
        if (!accountRecoveryService.canRecover(user.getEmail())) {
            throw new OAuth2AuthenticationException("탈퇴한 계정입니다.");
        }

        // 원문 제공자 ID를 브라우저에 노출하지 않고 일회용 티켓에만 보관한다.
        String ticket = accountRecoveryTicketService.issueOAuth(user.getId(), provider, providerUserId);
        throw new OAuth2RecoveryRequiredException(ticket);
    }

    private User updateProfileImage(User user, String imageUrl) {
        if (imageUrl == null || imageUrl.equals(user.getProfileImageUrl())) {
            return user;
        }

        User updatedUser = user.toBuilder().profileImageUrl(imageUrl).build();
        return userRepository.save(updatedUser);
    }
}
