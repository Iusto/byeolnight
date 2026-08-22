package com.byeolnight.service.auth;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth 계정 판별 서비스 테스트")
class OAuthAccountServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SocialUserRegistrationService socialUserRegistrationService;
    @Mock
    private AccountRecoveryService accountRecoveryService;
    @Mock
    private AccountRecoveryTicketService accountRecoveryTicketService;
    @Mock
    private OAuth2UserInfoFactory.OAuth2UserInfo userInfo;

    @InjectMocks
    private OAuthAccountService oAuthAccountService;

    @BeforeEach
    void setUpUserInfo() {
        when(userInfo.getProviderUserId()).thenReturn("google-subject");
        when(userInfo.getEmail()).thenReturn("member@example.com");
        when(userInfo.isEmailVerified()).thenReturn(true);
    }

    @Test
    @DisplayName("제공자 고유 ID를 이메일보다 먼저 조회한다")
    void authenticate_ProviderIdentityExists_UsesItFirst() {
        User user = activeSocialUser();
        when(userRepository.findBySocialProviderAndSocialProviderId("google", "google-subject"))
                .thenReturn(Optional.of(user));

        User result = oAuthAccountService.authenticate("google", userInfo);

        assertSame(user, result);
        verify(userRepository, never()).findByEmail("member@example.com");
    }

    @Test
    @DisplayName("레거시 소셜 회원은 재인증된 제공자 고유 ID를 연결한다")
    void authenticate_LegacySocialUser_LinksProviderIdentity() {
        User legacyUser = User.builder()
                .id(2L)
                .email("member@example.com")
                .nickname("레거시")
                .status(User.UserStatus.ACTIVE)
                .build();
        legacyUser.setSocialProvider("google");
        when(userRepository.findBySocialProviderAndSocialProviderId("google", "google-subject"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(legacyUser));

        oAuthAccountService.authenticate("google", userInfo);

        assertEquals("google-subject", legacyUser.getSocialProviderId());
    }

    @Test
    @DisplayName("기존 회원이 없으면 신규 소셜 가입 서비스에 위임한다")
    void authenticate_NewUser_DelegatesRegistration() {
        User registeredUser = activeSocialUser();
        when(userRepository.findBySocialProviderAndSocialProviderId("google", "google-subject"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.empty());
        when(socialUserRegistrationService.register("google", userInfo)).thenReturn(registeredUser);

        User result = oAuthAccountService.authenticate("google", userInfo);

        assertSame(registeredUser, result);
    }

    @Test
    @DisplayName("복구 가능한 탈퇴 계정은 원문 식별자 대신 일회용 티켓을 발급한다")
    void authenticate_WithdrawnUser_IssuesRecoveryTicket() {
        User user = User.builder()
                .id(3L)
                .email("member@example.com")
                .nickname("탈퇴회원")
                .status(User.UserStatus.WITHDRAWN)
                .withdrawnAt(LocalDateTime.now().minusDays(3))
                .build();
        user.linkSocialIdentity("google", "google-subject");
        when(userRepository.findBySocialProviderAndSocialProviderId("google", "google-subject"))
                .thenReturn(Optional.of(user));
        when(accountRecoveryService.canRecover("member@example.com")).thenReturn(true);
        when(accountRecoveryTicketService.issueOAuth(3L, "google", "google-subject"))
                .thenReturn("one-time-ticket");

        OAuth2RecoveryRequiredException exception = assertThrows(
                OAuth2RecoveryRequiredException.class,
                () -> oAuthAccountService.authenticate("google", userInfo)
        );

        assertEquals("one-time-ticket", exception.getRecoveryTicket());
    }

    private User activeSocialUser() {
        User user = User.builder()
                .id(1L)
                .email("member@example.com")
                .nickname("별회원")
                .status(User.UserStatus.ACTIVE)
                .build();
        user.linkSocialIdentity("google", "google-subject");
        return user;
    }
}
