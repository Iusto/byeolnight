package com.byeolnight.service.auth;

import com.byeolnight.dto.user.LoginRequestDto;
import com.byeolnight.entity.log.AuditLoginLog;
import com.byeolnight.entity.log.AuditSignupLog;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.security.JwtTokenProvider;
import com.byeolnight.repository.log.AuditLoginLogRepository;
import com.byeolnight.repository.log.AuditSignupLogRepository;
import com.byeolnight.service.certificate.CertificateService;
import com.byeolnight.service.user.UserAccountService;
import com.byeolnight.service.user.UserAdminService;
import com.byeolnight.service.user.UserQueryService;
import com.byeolnight.service.user.UserSecurityService;
import com.byeolnight.common.TestMockConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private UserAdminService userAdminService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuditLoginLogRepository auditLoginLogRepository;

    @Mock
    private AuditSignupLogRepository auditSignupLogRepository;

    @Mock
    private UserSecurityService userSecurityService;

    @Mock
    private CertificateService certificateService;

    @Mock
    private SocialAccountCleanupService socialAccountCleanupService;

    @Mock
    private HttpServletRequest request;

    private User testUser;
    private LoginRequestDto loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPassword")
                .nickname("testUser")
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .loginFailCount(0)
                .accountLocked(false)
                .createdAt(LocalDateTime.now())
                .build();

        loginRequest = LoginRequestDto.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        TestMockConfig.setupHttpServletRequest(request);
    }

    @Nested
    @DisplayName("로그인 성공 시나리오")
    class LoginSuccessScenarios {

        @Test
        @DisplayName("정상 로그인 - 토큰 생성 및 감사 로그 기록")
        void authenticate_Success_GeneratesTokensAndLogsAudit() {
            // Given
            given(userSecurityService.isIpBlocked(TestMockConfig.getTestIp())).willReturn(false);
            given(userQueryService.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
            given(userAccountService.checkPassword("password123", testUser)).willReturn(true);
            given(jwtTokenProvider.generateTokens(testUser, TestMockConfig.getTestUserAgent(), TestMockConfig.getTestIp()))
                    .willReturn(new String[]{"accessToken", "refreshToken"});

            // When
            AuthService.LoginResult result = authService.authenticate(loginRequest, request);

            // Then
            assertThat(result.getAccessToken()).isEqualTo("accessToken");
            assertThat(result.getRefreshToken()).isEqualTo("refreshToken");
            assertThat(result.getRefreshTokenValidity()).isEqualTo(7 * 24 * 60 * 60 * 1000L);

            verify(userAdminService).resetLoginFailCount(testUser);
            verify(auditLoginLogRepository).save(any(AuditLoginLog.class));
            verify(certificateService).checkAndIssueCertificates(testUser, CertificateService.CertificateCheckType.LOGIN);
        }
    }

    @Nested
    @DisplayName("로그인 실패 시나리오")
    class LoginFailureScenarios {

        @Test
        @DisplayName("존재하지 않는 이메일 - BadCredentialsException")
        void authenticate_NonExistentEmail_ThrowsException() {
            // Given
            given(userSecurityService.isIpBlocked(TestMockConfig.getTestIp())).willReturn(false);
            given(userQueryService.findByEmail("test@example.com")).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authService.authenticate(loginRequest, request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");

            verify(auditSignupLogRepository).save(any(AuditSignupLog.class));
        }

        @Test
        @DisplayName("잘못된 비밀번호 - 실패 횟수 증가")
        void authenticate_WrongPassword_IncrementsFailCount() {
            // Given
            given(userSecurityService.isIpBlocked(TestMockConfig.getTestIp())).willReturn(false);
            given(userQueryService.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
            given(userAccountService.checkPassword("wrongPassword", testUser)).willReturn(false);

            LoginRequestDto wrongPasswordRequest = LoginRequestDto.builder()
                    .email("test@example.com")
                    .password("wrongPassword")
                    .build();

            // When & Then
            assertThatThrownBy(() -> authService.authenticate(wrongPasswordRequest, request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("비밀번호가 올바르지 않습니다");

            verify(userAdminService).increaseLoginFailCount(testUser, TestMockConfig.getTestIp(), TestMockConfig.getTestUserAgent());
        }
    }

    @Nested
    @DisplayName("IP 차단 정책")
    class IpBlockingPolicy {

        @Test
        @DisplayName("차단된 IP에서 로그인 시도 - SecurityException")
        void authenticate_BlockedIp_ThrowsSecurityException() {
            // Given
            given(userSecurityService.isIpBlocked(TestMockConfig.getTestIp())).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> authService.authenticate(loginRequest, request))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("🚫 해당 IP는 비정상적인 로그인 시도")
                    .hasMessageContaining("1시간 차단되었습니다");

            verify(userQueryService, never()).findByEmail(anyString());
        }
    }
}
