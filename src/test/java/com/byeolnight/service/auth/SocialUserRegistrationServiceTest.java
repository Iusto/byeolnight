package com.byeolnight.service.auth;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.service.certificate.CertificateService;
import com.byeolnight.service.user.DefaultIconService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("소셜 신규 회원 가입 서비스 테스트")
class SocialUserRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DefaultIconService defaultIconService;
    @Mock
    private CertificateService certificateService;
    @Mock
    private OAuth2UserInfoFactory.OAuth2UserInfo userInfo;

    @InjectMocks
    private SocialUserRegistrationService registrationService;

    @Test
    @DisplayName("신규 소셜 회원을 저장하고 기본 아이콘과 인증서를 지급한다")
    void register_ValidProviderUser_CreatesAndInitializesUser() {
        when(userInfo.getEmail()).thenReturn("spaceuser@example.com");
        when(userInfo.getProviderUserId()).thenReturn("google-subject");
        when(userInfo.getImageUrl()).thenReturn("https://example.com/profile.png");
        when(userRepository.existsByNickname("spaceuse")).thenReturn(false);
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = registrationService.register("google", userInfo);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertSame(savedUser, result);
        assertEquals("spaceuser@example.com", savedUser.getEmail());
        assertEquals("spaceuse", savedUser.getNickname());
        assertEquals("google", savedUser.getSocialProvider());
        assertEquals("google-subject", savedUser.getSocialProviderId());
        assertNull(savedUser.getPassword());
        verify(defaultIconService).grant(savedUser);
        verify(certificateService).checkAndIssueCertificates(
                savedUser, CertificateService.CertificateCheckType.LOGIN);
    }
}
