package com.byeolnight.controller.auth;

import com.byeolnight.dto.auth.EmailRequestDto;
import com.byeolnight.dto.auth.EmailVerifyRequestDto;
import com.byeolnight.dto.user.LoginRequestDto;
import com.byeolnight.dto.user.UserSignUpRequestDto;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.security.JwtTokenProvider;
import com.byeolnight.repository.log.AuditRefreshTokenLogRepository;
import com.byeolnight.service.auth.AuthService;
import com.byeolnight.service.auth.EmailAuthService;
import com.byeolnight.service.auth.PasswordResetService;
import com.byeolnight.service.auth.SocialAccountCleanupService;
import com.byeolnight.service.auth.TokenService;
import com.byeolnight.service.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 통합 테스트 - 실제 HTTP 요청/응답 검증
 * 
 * 테스트 범위:
 * 1. 로그인/로그아웃 API
 * 2. 토큰 재발급 API
 * 3. 이메일 인증 API
 * 4. 회원가입 API
 * 5. 비밀번호 재설정 API
 * 6. 쿠키 처리 및 보안 헤더
 */
@WebMvcTest(AuthController.class)
@DisplayName("AuthController 통합 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UserService userService;

    @MockBean
    private EmailAuthService emailAuthService;

    @MockBean
    private PasswordResetService passwordResetService;

    @MockBean
    private AuditRefreshTokenLogRepository auditRefreshTokenLogRepository;

    @MockBean
    private SocialAccountCleanupService socialAccountCleanupService;

    private User testUser;
    private LoginRequestDto loginRequest;
    private final String TEST_ACCESS_TOKEN = "test.access.token";
    private final String TEST_REFRESH_TOKEN = "test.refresh.token";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPassword")
                .nickname("testUser")
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        loginRequest = LoginRequestDto.builder()
                .email("test@example.com")
                .password("password123")
                .build();
    }

    @Nested
    @DisplayName("로그인 API")
    class LoginAPI {

        @Test
        @DisplayName("정상 로그인 - 토큰 및 쿠키 반환")
        void login_Success_ReturnsTokensAndCookies() throws Exception {
            // Given
            AuthService.LoginResult loginResult = new AuthService.LoginResult(
                    TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN, 604800000L);
            given(authService.authenticate(any(LoginRequestDto.class), any())).willReturn(loginResult);

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value(TEST_ACCESS_TOKEN))
                    .andExpect(jsonPath("$.data.authenticated").value(true))
                    .andExpect(header().exists("Set-Cookie"));
        }

        @Test
        @DisplayName("잘못된 자격증명 - 401 Unauthorized")
        void login_BadCredentials_ReturnsUnauthorized() throws Exception {
            // Given
            given(authService.authenticate(any(LoginRequestDto.class), any()))
                    .willThrow(new BadCredentialsException("비밀번호가 올바르지 않습니다."));

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("비밀번호가 올바르지 않습니다."));
        }

        @Test
        @DisplayName("IP 차단 - 403 Forbidden")
        void login_BlockedIp_ReturnsForbidden() throws Exception {
            // Given
            given(authService.authenticate(any(LoginRequestDto.class), any()))
                    .willThrow(new SecurityException("IP가 차단되었습니다."));

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("IP가 차단되었습니다."));
        }

        @Test
        @DisplayName("유효하지 않은 요청 데이터 - 400 Bad Request")
        void login_InvalidRequest_ReturnsBadRequest() throws Exception {
            // Given
            LoginRequestDto invalidRequest = LoginRequestDto.builder()
                    .email("invalid-email") // 잘못된 이메일 형식
                    .password("")           // 빈 비밀번호
                    .build();

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("토큰 재발급 API")
    class TokenRefreshAPI {

        @Test
        @DisplayName("유효한 Refresh Token - 새 토큰 발급")
        void refreshToken_ValidToken_ReturnsNewTokens() throws Exception {
            // Given
            given(jwtTokenProvider.validateRefreshToken(TEST_REFRESH_TOKEN)).willReturn(true);
            given(jwtTokenProvider.getEmail(TEST_REFRESH_TOKEN)).willReturn("test@example.com");
            given(userService.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
            given(jwtTokenProvider.createAccessToken(testUser)).willReturn("new.access.token");
            given(jwtTokenProvider.createRefreshToken(testUser)).willReturn("new.refresh.token");
            given(jwtTokenProvider.getRefreshTokenValidity()).willReturn(604800000L);

            // When & Then
            mockMvc.perform(post("/api/auth/token/refresh")
                    .with(csrf())
                    .cookie(new Cookie("refreshToken", TEST_REFRESH_TOKEN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("new.access.token"))
                    .andExpect(header().exists("Set-Cookie"));

            verify(tokenService).saveRefreshToken(eq("test@example.com"), eq("new.refresh.token"), eq(604800000L));
        }

        @Test
        @DisplayName("유효하지 않은 Refresh Token - 401 Unauthorized")
        void refreshToken_InvalidToken_ReturnsUnauthorized() throws Exception {
            // Given
            given(jwtTokenProvider.validateRefreshToken(TEST_REFRESH_TOKEN)).willReturn(false);

            // When & Then
            mockMvc.perform(post("/api/auth/token/refresh")
                    .with(csrf())
                    .cookie(new Cookie("refreshToken", TEST_REFRESH_TOKEN)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("유효하지 않은 Refresh Token"));
        }

        @Test
        @DisplayName("Refresh Token 쿠키 없음 - 401 Unauthorized")
        void refreshToken_NoCookie_ReturnsUnauthorized() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/auth/token/refresh")
                    .with(csrf()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("존재하지 않는 사용자 - 401 Unauthorized")
        void refreshToken_UserNotFound_ReturnsUnauthorized() throws Exception {
            // Given
            given(jwtTokenProvider.validateRefreshToken(TEST_REFRESH_TOKEN)).willReturn(true);
            given(jwtTokenProvider.getEmail(TEST_REFRESH_TOKEN)).willReturn("test@example.com");
            given(userService.findByEmail("test@example.com")).willReturn(Optional.empty());

            // When & Then
            mockMvc.perform(post("/api/auth/token/refresh")
                    .with(csrf())
                    .cookie(new Cookie("refreshToken", TEST_REFRESH_TOKEN)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("인증 정보가 유효하지 않습니다. 다시 로그인해주세요."));

            verify(tokenService).deleteRefreshToken("test@example.com");
        }
    }

    @Nested
    @DisplayName("로그아웃 API")
    class LogoutAPI {

        @Test
        @WithMockUser
        @DisplayName("정상 로그아웃 - 토큰 무효화 및 쿠키 삭제")
        void logout_Success_InvalidatesTokensAndDeletesCookies() throws Exception {
            // Given
            given(jwtTokenProvider.validate(TEST_ACCESS_TOKEN)).willReturn(true);
            given(jwtTokenProvider.getExpiration(TEST_ACCESS_TOKEN)).willReturn(1800000L);

            // When & Then
            mockMvc.perform(post("/api/auth/logout")
                    .with(csrf())
                    .cookie(new Cookie("accessToken", TEST_ACCESS_TOKEN))
                    .cookie(new Cookie("refreshToken", TEST_REFRESH_TOKEN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value("로그아웃되었습니다."))
                    .andExpect(header().exists("Set-Cookie"));

            verify(tokenService).blacklistAccessToken(TEST_ACCESS_TOKEN, 1800000L);
        }

        @Test
        @DisplayName("토큰 없이 로그아웃 - 성공 (안전한 처리)")
        void logout_NoTokens_ReturnsSuccess() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/auth/logout")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value("로그아웃되었습니다."));
        }
    }

    @Nested
    @DisplayName("이메일 인증 API")
    class EmailAuthAPI {

        @Test
        @DisplayName("이메일 인증 코드 전송 - 성공")
        void sendEmailCode_Success_ReturnsSuccess() throws Exception {
            // Given
            String emailRequestJson = "{\"email\":\"test@example.com\"}";
            doNothing().when(emailAuthService).sendCode("test@example.com");

            // When & Then
            mockMvc.perform(post("/api/auth/email/send")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(emailRequestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value("이메일 인증 코드가 전송되었습니다."));
        }

        @Test
        @DisplayName("이미 인증된 이메일 - 400 Bad Request")
        void sendEmailCode_AlreadyVerified_ReturnsBadRequest() throws Exception {
            // Given
            String emailRequestJson = "{\"email\":\"test@example.com\"}";
            doThrow(new IllegalStateException("이미 인증이 완료된 이메일입니다."))
                    .when(emailAuthService).sendCode("test@example.com");

            // When & Then
            mockMvc.perform(post("/api/auth/email/send")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(emailRequestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("이미 인증이 완료된 이메일입니다."));
        }

        @Test
        @DisplayName("이메일 인증 코드 검증 - 성공")
        void verifyEmailCode_Success_ReturnsTrue() throws Exception {
            // Given
            String verifyRequestJson = "{\"email\":\"test@example.com\",\"code\":\"ABC12345\"}";
            given(emailAuthService.verifyCode(eq("test@example.com"), eq("ABC12345"), anyString()))
                    .willReturn(true);

            // When & Then
            mockMvc.perform(post("/api/auth/email/verify")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(verifyRequestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(true));
        }

        @Test
        @DisplayName("잘못된 인증 코드 - 성공하지만 false 반환")
        void verifyEmailCode_InvalidCode_ReturnsFalse() throws Exception {
            // Given
            String verifyRequestJson = "{\"email\":\"test@example.com\",\"code\":\"WRONG123\"}";
            given(emailAuthService.verifyCode(eq("test@example.com"), eq("WRONG123"), anyString()))
                    .willReturn(false);

            // When & Then
            mockMvc.perform(post("/api/auth/email/verify")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(verifyRequestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(false));
        }

        @Test
        @DisplayName("시도 횟수 초과 - 429 Too Many Requests")
        void verifyEmailCode_TooManyAttempts_ReturnsTooManyRequests() throws Exception {
            // Given
            String verifyRequestJson = "{\"email\":\"test@example.com\",\"code\":\"ABC12345\"}";
            doThrow(new IllegalStateException("인증 시도 횟수를 초과했습니다."))
                    .when(emailAuthService).verifyCode(eq("test@example.com"), eq("ABC12345"), anyString());

            // When & Then
            mockMvc.perform(post("/api/auth/email/verify")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(verifyRequestJson))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("인증 시도 횟수를 초과했습니다."));
        }

        @Test
        @DisplayName("이메일 인증 상태 확인 - 인증됨")
        void checkEmailStatus_Verified_ReturnsTrue() throws Exception {
            // Given
            given(emailAuthService.isAlreadyVerified("test@example.com")).willReturn(true);

            // When & Then
            mockMvc.perform(get("/api/auth/email/status")
                    .param("email", "test@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(true));
        }

        @Test
        @DisplayName("이메일 데이터 정리 - 성공")
        void cleanupEmailData_Success_ReturnsSuccess() throws Exception {
            // Given
            String emailRequestJson = "{\"email\":\"test@example.com\"}";
            doNothing().when(emailAuthService).clearAllEmailData("test@example.com");

            // When & Then
            mockMvc.perform(delete("/api/auth/email/cleanup")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(emailRequestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value("이메일 인증 데이터가 정리되었습니다."));
        }
    }

    @Nested
    @DisplayName("회원가입 API")
    class SignupAPI {

        @Test
        @DisplayName("정상 회원가입 - 성공")
        void signup_Success_ReturnsSuccess() throws Exception {
            // Given
            UserSignUpRequestDto signupRequest = UserSignUpRequestDto.builder()
                    .email("test@example.com")
                    .password("password123!")
                    .nickname("testUser")
                    .build();

            given(emailAuthService.isAlreadyVerified("test@example.com")).willReturn(true);
            doNothing().when(userService).createUser(any(UserSignUpRequestDto.class), any());
            doNothing().when(emailAuthService).clearVerificationStatus("test@example.com");

            // When & Then
            mockMvc.perform(post("/api/auth/signup")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signupRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value("회원가입이 완료되었습니다."));

            verify(emailAuthService).clearVerificationStatus("test@example.com");
        }

        @Test
        @DisplayName("이메일 인증 미완료 - 400 Bad Request")
        void signup_EmailNotVerified_ReturnsBadRequest() throws Exception {
            // Given
            UserSignUpRequestDto signupRequest = UserSignUpRequestDto.builder()
                    .email("test@example.com")
                    .password("password123!")
                    .nickname("testUser")
                    .build();

            given(emailAuthService.isAlreadyVerified("test@example.com")).willReturn(false);

            // When & Then
            mockMvc.perform(post("/api/auth/signup")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signupRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("이메일 인증이 필요합니다."));

            verify(userService, never()).createUser(any(), any());
        }
    }

    @Nested
    @DisplayName("닉네임 중복 확인 API")
    class NicknameCheckAPI {

        @Test
        @DisplayName("사용 가능한 닉네임 - true 반환")
        void checkNickname_Available_ReturnsTrue() throws Exception {
            // Given
            given(userService.existsByNickname("newUser")).willReturn(false);

            // When & Then
            mockMvc.perform(get("/api/auth/check-nickname")
                    .param("value", "newUser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(true));
        }

        @Test
        @DisplayName("이미 사용 중인 닉네임 - false 반환")
        void checkNickname_Taken_ReturnsFalse() throws Exception {
            // Given
            given(userService.existsByNickname("existingUser")).willReturn(true);

            // When & Then
            mockMvc.perform(get("/api/auth/check-nickname")
                    .param("value", "existingUser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(false));
        }
    }

    @Nested
    @DisplayName("인증 상태 확인 API")
    class AuthCheckAPI {

        @Test
        @WithMockUser
        @DisplayName("인증된 사용자 - 성공")
        void checkAuth_Authenticated_ReturnsSuccess() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/auth/check"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value("인증됨"));
        }

        @Test
        @DisplayName("인증되지 않은 사용자 - 401 Unauthorized")
        void checkAuth_NotAuthenticated_ReturnsUnauthorized() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/auth/check"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("인증되지 않음"));
        }
    }
}