package com.byeolnight.service.auth;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.RestOperations;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2 사용자 서비스 회귀 테스트")
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SocialUserRegistrationService socialUserRegistrationService;
    @Mock
    private AccountRecoveryService accountRecoveryService;
    @Mock
    private AccountRecoveryTicketService accountRecoveryTicketService;
    @Mock
    private RestOperations restOperations;

    private CustomOAuth2UserService service;

    @BeforeEach
    void setUp() {
        OAuthAccountService oAuthAccountService = new OAuthAccountService(
                userRepository,
                socialUserRegistrationService,
                accountRecoveryService,
                accountRecoveryTicketService
        );
        service = new CustomOAuth2UserService(oAuthAccountService);
        service.setRestOperations(restOperations);
    }

    @Test
    @DisplayName("제공자 고유 ID로 기존 회원을 찾으면 이메일 조회 없이 로그인한다")
    void loadUser_ExistingProviderIdentity_UsesProviderIdFirst() {
        // given
        User existingUser = User.builder()
                .id(1L)
                .email("member@example.com")
                .nickname("별회원")
                .status(User.UserStatus.ACTIVE)
                .profileImageUrl("https://example.com/profile.png")
                .build();
        existingUser.linkSocialIdentity("google", "google-subject");

        when(restOperations.exchange(
                any(RequestEntity.class),
                org.mockito.ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(ResponseEntity.ok(Map.of(
                "sub", "google-subject",
                "email", "member@example.com",
                "email_verified", true,
                "name", "별회원",
                "picture", "https://example.com/profile.png"
        )));
        when(userRepository.findBySocialProviderAndSocialProviderId("google", "google-subject"))
                .thenReturn(Optional.of(existingUser));

        // when
        OAuth2User result = service.loadUser(createGoogleRequest());

        // then
        assertSame(existingUser, ((CustomOAuth2User) result).getUser());
        assertEquals("member@example.com", result.getName());
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    private OAuth2UserRequest createGoogleRequest() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "email", "profile")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token",
                Instant.now(),
                Instant.now().plusSeconds(300)
        );
        return new OAuth2UserRequest(registration, accessToken);
    }
}
