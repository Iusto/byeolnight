package com.byeolnight.service.auth;

import com.byeolnight.dto.user.LoginRequestDto;
import com.byeolnight.entity.log.AuditLoginLog;
import com.byeolnight.entity.log.AuditSignupLog;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.security.JwtTokenProvider;
import com.byeolnight.repository.log.AuditLoginLogRepository;
import com.byeolnight.repository.log.AuditSignupLogRepository;
import com.byeolnight.service.certificate.CertificateService;
import com.byeolnight.service.user.UserSecurityService;
import com.byeolnight.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * AuthService 테스트 - 실제 비즈니스 로직 검증에 집중
 * 
 * 테스트 범위:
 * 1. 로그인 성공/실패 시나리오
 * 2. 계정 보안 정책 (잠금, 실패 횟수)
 * 3. 소셜 로그인 사용자 처리
 * 4. IP 차단 정책
 * 5. 감사 로그 기록
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserService userService;
    
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
    private HttpServletRequest request;

    private User testUser;
    private LoginRequestDto loginRequest;
    private final String TEST_IP = "192.168.1.1";
    private final String TEST_USER_AGENT = "Mozilla/5.0 Test Browser";

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

        // 공통 Mock 설정
        given(request.getHeader("User-Agent")).willReturn(TEST_USER_AGENT);
        given(request.getRemoteAddr()).willReturn(TEST_IP);
    }

    @Nested
    @DisplayName("로그인 성공 시나리오")
    class LoginSuccessScenarios {

        @Test
        @DisplayName("정상 로그인 - 토큰 생성 및 감사 로그 기록")
        void authenticate_Success_GeneratesTokensAndLogsAudit() {
            // Given
            given(userSecurityService.isIpBlocked(TEST_IP)).willReturn(false);
            given(userService.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
            given(userService.checkPassword("password123", testUser)).willReturn(true);
            given(jwtTokenProvider.generateTokens(1L, TEST_USER_AGENT, TEST_IP))
                    .willReturn(new String[]{"accessToken", "refreshToken"});

            // When
            AuthService.LoginResult result = authService.authenticate(loginRequest, request);

            // Then
            assertThat(result.getAccessToken()).isEqualTo("accessToken");
            assertThat(result.getRefreshToken()).isEqualTo("refreshToken");
            assertThat(result.getRefreshTokenValidity()).isEqualTo(7 * 24 * 60 * 60 * 1000L);

            // 로그인 실패 횟수 초기화 확인
            verify(userService).resetLoginFailCount(testUser);
            
            // 감사 로그 기록 확인
            verify(auditLoginLogRepository).save(any(AuditLoginLog.class));
            
            // 인증서 발급 체크 확인
            verify(certificateService).checkAndIssueCertificates(testUser, CertificateService.CertificateCheckType.LOGIN);
        }

        @Test
        @DisplayName("로그인 실패 횟수가 있던 사용자의 성공 로그인 - 실패 횟수 초기화")
        void authenticate_Success_ResetsFailCount() {
            // Given
            User userWithFailCount = testUser.toBuilder()
                    .loginFailCount(3)
                    .lastFailedLogin(LocalDateTime.now().minusMinutes(10))
                    .build();
            
            given(userSecurityService.isIpBlocked(TEST_IP)).willReturn(false);
            given(userService.findByEmail("test@example.com")).willReturn(Optional.of(userWithFailCount));
            given(userService.checkPassword("password123", userWithFailCount)).willReturn(true);
            given(jwtTokenProvider.generateTokens(1L, TEST_USER_AGENT, TEST_IP))
                    .willReturn(new String[]{"accessToken", "refreshToken"});

            // When
            AuthService.LoginResult result = authService.authenticate(loginRequest, request);

            // Then
            assertThat(result).isNotNull();
            verify(userService).resetLoginFailCount(userWithFailCount);
        }
    }

    @Nested
    @DisplayName("로그인 실패 시나리오")
    class LoginFailureScenarios {

        @Test
        @DisplayName("존재하지 않는 이메일 - BadCredentialsException 및 감사 로그")
        void authenticate_NonExistentEmail_ThrowsExceptionAndLogsFailure() {
            // Given
            given(userSecurityService.isIpBlocked(TEST_IP)).willReturn(false);
            given(userService.findByEmail("test@example.com")).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authService.authenticate(loginRequest, request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("존재하지 않는 아이디입니다.");

            // 실패 감사 로그 기록 확인
            verify(auditSignupLogRepository).save(any(AuditSignupLog.class));
        }

        @Test
        @DisplayName("잘못된 비밀번호 - 실패 횟수 증가 및 경고 메시지")
        void authenticate_WrongPassword_IncrementsFailCountAndShowsWarning() {
            // Given
            given(userSecurityService.isIpBlocked(TEST_IP)).willReturn(false);
            given(userService.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
            given(userService.checkPassword("wrongPassword", testUser)).willReturn(false);

            LoginRequestDto wrongPasswordRequest = LoginRequestDto.builder()
                    .email("test@example.com")
                    .password("wrongPassword")
                    .build();

            // When & Then
            assertThatThrownBy(() -> authService.authenticate(wrongPasswordRequest, request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("비밀번호가 올바르지 않습니다");

            // 실패 횟수 증가 확인
            verify(userService).increaseLoginFailCount(testUser, TEST_IP, TEST_USER_AGENT);
        }

        @Test
        @DisplayName("5회 실패 후 로그인 시도 - 경고 메시지 포함")
        void authenticate_FifthFailure_ShowsWarningMessage() {
            // Given
            User userWith4Failures = testUser.toBuilder()
                    .loginFailCount(4) // 5번째 실패가 될 예정
                    .build();
            
            given(userSecurityService.isIpBlocked(TEST_IP)).willReturn(false);
            given(userService.findByEmail("test@example.com")).willReturn(Optional.of(userWith4Failures));
            given(userService.checkPassword("wrongPassword", userWith4Failures)).willReturn(false);

            LoginRequestDto wrongPasswordRequest = LoginRequestDto.builder()
                    .email("test@example.com")
                    .password("wrongPassword")
                    .build();

            // When & Then
            assertThatThrownBy(() -> authService.authenticate(wrongPasswordRequest, request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("⚠️ 경고")
                    .hasMessageContaining("5회 더 틀리면 계정이 잠깁니다");
        }

        @Test
        @DisplayName("10회 실패 후 로그인 시도 - 계정 잠금")
        void authenticate_TenthFailure_LocksAccount() {
            // Given
            User userWith9Failures = testUser.toBuilder()
                    .loginFailCount(9) // 10번째 실패가 될 예정
                    .build();
            
            given(userSecurityService.isIpBlocked(TEST_IP)).willReturn(false);
            given(userService.findByEmail("test@example.com")).willReturn(Optional.of(userWith9Failures));
            given(userService.checkPassword("wrongPassword", userWith9Failures)).willReturn(false);

            LoginRequestDto wrongPasswordRequest = LoginRequestDto.builder()
                    .email("test@example.com")
                    .password("wrongPassword")
                    .build();

            // When & Then
            assertThatThrownBy(() -> authService.authenticate(wrongPasswordRequest, request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("계정이 잠겼습니다")
                    .hasMessageContaining("비밀번호를 초기화해야 잠금이 해제됩니다");
        }

        @Test
        @DisplayName("15회 실패 후 로그인 시도 - IP 차단 메시지")
        void authenticate_FifteenthFailure_ShowsIpBlockMessage() {
            // Given
            User userWith14Failures = testUser.toBuilder()
                    .loginFailCount(14) // 15번째 실패가 될 예정
                    .build();
            
            given(userSecurityService.isIpBlocked(TEST_IP)).willReturn(false);
            given(userService.findByEmail("test@example.com")).willReturn(Optional.of(userWith14Failures));
            given(userService.checkPassword("wrongPassword", userWith14Failures)).willReturn(false);

            LoginRequestDto wrongPasswordRequest = LoginRequestDto.builder()
                    .email("test@example.com")
                    .password("wrongPassword")
                    .build();

            // When & Then
            assertThatThrownBy(() -> authService.authenticate(wrongPasswordRequest, request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("IP가 차단되었습니다");
        }
    }

    @Nested
    @DisplayName("계정 상태별 로그인 제한")
    class AccountStatusRestrictions {

        @Test
        @DisplayName("비활성 계정 로그인 시도 - 접근 거부")
        void authenticate_InactiveAccount_DeniesAccess() {
            // Given
            User inactiveUser = testUser.toBuilder()
                    .status(User.UserStatus.SUSPENDED)
                    .build();
            
            given(userSecurityService.isIpBlocked(TEST_IP)).willReturn(false);
            given(userService.findByEmail("test@example.com")).willReturn(Optional.of(inactiveUser));

            // When & Then
            assertThatThrownBy(() -> authService.authenticate(loginRequest, request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("해당 계정은 로그인할 수 없습니다")
                    .hasMessageContaining("SUSPENDED");

            // 실패 감사 로그 기록 확인
            verify(auditSignupLogRepository).save(any(AuditSignupLog.class));
        }

        @Test
        @DisplayName("잠긴 계정 로그인 시도 - 접근 거부")
        void authenticate_LockedAccount_DeniesAccess() {
            // Given
            User lockedUser = testUser.toBuilder()
                    .accountLocked(true)
                    .build();
            
            given(userSecurityService.isIpBlocked(TEST_IP)).willReturn(false);
            given(userService.findByEmail("test@example.com")).willReturn(Optional.of(lockedUser));

            // When & Then
            assertThatThrownBy(() -> authService.authenticate(loginRequest, request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("🔒 계정이 잠겨 있습니다")
                    .hasMessageContaining("비밀번호 초기화를 통해 잠금을 해제");
        }

        @Test
        @DisplayName("소셜 로그인 사용자의 일반 로그인 시도 - 소셜 로그인 안내")
        void authenticate_SocialUser_RedirectsToSocialLogin() {
            // Given
            User socialUser = testUser.toBuilder()
                    .password(null) // 소셜 사용자는 비밀번호 없음
                    .socialProvider("google")
                    .build();
            
            given(userSecurityService.isIpBlocked(TEST_IP)).willReturn(false);
            given(userService.findByEmail("test@example.com")).willReturn(Optional.of(socialUser));

            // When & Then
            assertThatThrownBy(() -> authService.authenticate(loginRequest, request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("구글 로그인으로 가입된 계정입니다")
                    .hasMessageContaining("구글 로그인을 이용해주세요");
        }
    }

    @Nested
    @DisplayName("IP 차단 정책")
    class IpBlockingPolicy {

        @Test
        @DisplayName("차단된 IP에서 로그인 시도 - SecurityException")
        void authenticate_BlockedIp_ThrowsSecurityException() {
            // Given
            given(userSecurityService.isIpBlocked(TEST_IP)).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> authService.authenticate(loginRequest, request))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("🚫 해당 IP는 비정상적인 로그인 시도")
                    .hasMessageContaining("1시간 차단되었습니다");

            // 사용자 조회 자체가 실행되지 않음을 확인
            verify(userService, never()).findByEmail(anyString());
        }
    }

    @Nested
    @DisplayName("OAuth 사용자 처리")
    class OAuthUserHandling {

        @Test
        @DisplayName("닉네임 설정 필요 여부 확인 - 빈 닉네임")
        void needsNicknameSetup_EmptyNickname_ReturnsTrue() {
            // Given
            User userWithEmptyNickname = testUser.toBuilder()
                    .nickname("")
                    .build();
            given(userService.findById(1L)).willReturn(userWithEmptyNickname);

            // When
            boolean needsSetup = authService.needsNicknameSetup(1L);

            // Then
            assertThat(needsSetup).isTrue();
        }

        @Test
        @DisplayName("닉네임 설정 필요 여부 확인 - null 닉네임")
        void needsNicknameSetup_NullNickname_ReturnsTrue() {
            // Given
            User userWithNullNickname = testUser.toBuilder()
                    .nickname(null)
                    .build();
            given(userService.findById(1L)).willReturn(userWithNullNickname);

            // When
            boolean needsSetup = authService.needsNicknameSetup(1L);

            // Then
            assertThat(needsSetup).isTrue();
        }

        @Test
        @DisplayName("닉네임 설정 필요 여부 확인 - 정상 닉네임")
        void needsNicknameSetup_ValidNickname_ReturnsFalse() {
            // Given
            given(userService.findById(1L)).willReturn(testUser);

            // When
            boolean needsSetup = authService.needsNicknameSetup(1L);

            // Then
            assertThat(needsSetup).isFalse();
        }
    }

    @Nested
    @DisplayName("LoginResult 내부 클래스")
    class LoginResultTest {

        @Test
        @DisplayName("LoginResult 생성 및 getter 테스트")
        void loginResult_CreationAndGetters() {
            // Given
            String accessToken = "access.token.here";
            String refreshToken = "refresh.token.here";
            long validity = 604800000L;

            // When
            AuthService.LoginResult result = new AuthService.LoginResult(accessToken, refreshToken, validity);

            // Then
            assertThat(result.getAccessToken()).isEqualTo(accessToken);
            assertThat(result.getRefreshToken()).isEqualTo(refreshToken);
            assertThat(result.getRefreshTokenValidity()).isEqualTo(validity);
        }
    }
}