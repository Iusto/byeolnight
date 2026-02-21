package com.byeolnight.controller.auth;

import com.byeolnight.common.TestSecurityConfig;
import com.byeolnight.dto.user.LoginRequestDto;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.security.JwtAuthenticationFilter;
import com.byeolnight.infrastructure.security.JwtTokenProvider;
import com.byeolnight.infrastructure.security.SecurityConfig;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import com.byeolnight.repository.log.AuditRefreshTokenLogRepository;
import com.byeolnight.service.auth.AuthService;
import com.byeolnight.service.auth.EmailAuthService;
import com.byeolnight.service.auth.PasswordResetService;
import com.byeolnight.service.auth.SocialAccountCleanupService;
import com.byeolnight.service.auth.TokenService;
import com.byeolnight.service.certificate.CertificateService;
import com.byeolnight.service.user.UserAccountService;
import com.byeolnight.service.user.UserQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = AuthController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
        }
)
@Import(TestSecurityConfig.class)
@DisplayName("AuthController 테스트")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean AuthService authService;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean TokenService tokenService;
    @MockBean UserQueryService userQueryService;
    @MockBean UserAccountService userAccountService;
    @MockBean EmailAuthService emailAuthService;
    @MockBean PasswordResetService passwordResetService;
    @MockBean CertificateService certificateService;
    @MockBean AuditRefreshTokenLogRepository auditRefreshTokenLogRepository;
    @MockBean SocialAccountCleanupService socialAccountCleanupService;

    private final User mockUser = User.builder()
            .email("test@test.com")
            .nickname("tester")
            .password("encodedPassword")
            .role(User.Role.USER)
            .status(User.UserStatus.ACTIVE)
            .build();

    @Nested
    @DisplayName("로그인 POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("성공 시 200과 Set-Cookie 헤더 반환")
        void success_returns200WithCookies() throws Exception {
            AuthService.LoginResult loginResult = mock(AuthService.LoginResult.class);
            when(loginResult.getAccessToken()).thenReturn("mock-access-token");
            when(loginResult.getRefreshToken()).thenReturn("mock-refresh-token");
            when(loginResult.getRefreshTokenValidity()).thenReturn(604800000L);
            when(authService.authenticate(any(), any())).thenReturn(loginResult);

            LoginRequestDto dto = LoginRequestDto.builder()
                    .email("test@test.com")
                    .password("password123!")
                    .rememberMe(false)
                    .build();

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(header().exists("Set-Cookie"));
        }

        @Test
        @DisplayName("잘못된 비밀번호 시 401 반환")
        void wrongPassword_returns401() throws Exception {
            when(authService.authenticate(any(), any()))
                    .thenThrow(new BadCredentialsException("비밀번호가 올바르지 않습니다."));

            LoginRequestDto dto = LoginRequestDto.builder()
                    .email("test@test.com")
                    .password("wrongPassword")
                    .rememberMe(false)
                    .build();

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("차단된 IP 시 403 반환")
        void blockedIp_returns403() throws Exception {
            when(authService.authenticate(any(), any()))
                    .thenThrow(new SecurityException("⚠️ 경고: 해당 IP는 차단되었습니다."));

            LoginRequestDto dto = LoginRequestDto.builder()
                    .email("test@test.com")
                    .password("password123!")
                    .rememberMe(false)
                    .build();

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("로그아웃 POST /api/auth/logout")
    class Logout {

        @Test
        @DisplayName("로그인 상태에서 200과 쿠키 만료 반환")
        void authenticated_returns200WithExpiredCookies() throws Exception {
            mockMvc.perform(post("/api/auth/logout")
                            .with(user(mockUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("인증 상태 확인 GET /api/auth/check")
    class Check {

        @Test
        @DisplayName("로그인 시 200과 '인증됨' 반환")
        void authenticated_returns200() throws Exception {
            mockMvc.perform(get("/api/auth/check")
                            .with(user(mockUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("인증됨"));
        }

        @Test
        @DisplayName("비로그인 시 컨트롤러에서 401 반환")
        void unauthenticated_returns401() throws Exception {
            // /api/auth/** 는 Security 화이트리스트이므로 보안 레이어 통과
            // 컨트롤러 내부에서 user == null 검사 후 401 반환
            mockMvc.perform(get("/api/auth/check"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("현재 사용자 정보 GET /api/auth/me")
    class Me {

        @Test
        @DisplayName("비로그인 시 200과 null data 반환 (비로그인 허용 엔드포인트)")
        void unauthenticated_returns200WithNullData() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Nested
    @DisplayName("닉네임 중복 확인 GET /api/auth/check-nickname")
    class CheckNickname {

        @Test
        @DisplayName("사용 가능한 닉네임 시 true 반환")
        void available_returnsTrue() throws Exception {
            when(userQueryService.existsByNickname("uniqueNick")).thenReturn(false);

            mockMvc.perform(get("/api/auth/check-nickname")
                            .param("value", "uniqueNick"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(true));
        }

        @Test
        @DisplayName("중복 닉네임 시 false 반환")
        void duplicate_returnsFalse() throws Exception {
            when(userQueryService.existsByNickname("dupNick")).thenReturn(true);

            mockMvc.perform(get("/api/auth/check-nickname")
                            .param("value", "dupNick"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(false));
        }
    }

    @Nested
    @DisplayName("이메일 인증 POST /api/auth/email/send")
    class EmailSend {

        @Test
        @DisplayName("정상 전송 시 200 반환")
        void success_returns200() throws Exception {
            mockMvc.perform(post("/api/auth/email/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"test@test.com\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("전송 한도 초과 시 400 반환")
        void limitExceeded_returns400() throws Exception {
            doThrow(new IllegalStateException("인증 코드 전송 한도를 초과했습니다."))
                    .when(emailAuthService).sendCode(any());

            mockMvc.perform(post("/api/auth/email/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"test@test.com\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
