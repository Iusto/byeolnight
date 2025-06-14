package com.byeolnight.controller;

import com.byeolnight.domain.entity.log.AuditRefreshTokenLog;
import com.byeolnight.domain.repository.AuditRefreshTokenLogRepository;
import com.byeolnight.dto.user.*;
import com.byeolnight.service.auth.EmailAuthService;
import com.byeolnight.service.auth.PhoneAuthService;
import com.byeolnight.service.auth.TokenService;
import com.byeolnight.service.user.UserService;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.dto.auth.*;
import com.byeolnight.infrastructure.security.JwtTokenProvider;
import com.byeolnight.infrastructure.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailAuthService emailAuthService;
    private final PhoneAuthService phoneAuthService;
    private final TokenService tokenService;
    private final AuditRefreshTokenLogRepository auditRefreshTokenLogRepository;

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "로그인 실패"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    public ResponseEntity<CommonResponse<TokenResponseDto>> login(@RequestBody @Valid LoginRequestDto dto) {
        try {
            User user = userService.findByEmail(dto.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

            if (user.isAccountLocked()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.fail("계정이 잠겨 있습니다. 관리자에게 문의하세요."));
            }

            if (!userService.checkPassword(dto.getPassword(), user)) {
                userService.increaseLoginFailCount(user);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.fail("비밀번호가 일치하지 않습니다."));
            }

            userService.resetLoginFailCount(user);

            String accessToken = jwtTokenProvider.createAccessToken(user);
            String refreshToken = jwtTokenProvider.createRefreshToken(user);

            return ResponseEntity.ok(CommonResponse.success(new TokenResponseDto(accessToken, refreshToken)));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.fail(e.getMessage()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("서버 오류가 발생했습니다."));
        }
    }

    @PostMapping("/email/send")
    @Operation(summary = "이메일 인증 코드 발송", description = "이메일 주소로 인증 코드를 전송합니다.")
    public ResponseEntity<Void> sendEmailCode(@RequestBody @Valid EmailRequestDto dto) {
        emailAuthService.sendCode(dto.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/verify")
    @Operation(summary = "이메일 인증 코드 검증", description = "전송된 인증 코드를 검증합니다.")
    public ResponseEntity<Boolean> verifyEmailCode(@RequestBody @Valid EmailVerifyRequestDto dto) {
        boolean isValid = emailAuthService.verifyCode(dto.getEmail(), dto.getCode());
        return ResponseEntity.ok(isValid);
    }

    @PostMapping("/phone/send")
    @Operation(summary = "휴대폰 인증 코드 발송", description = "휴대폰 번호로 인증 코드를 전송합니다.")
    public ResponseEntity<Void> sendPhoneCode(@RequestBody @Valid PhoneRequestDto dto) {
        phoneAuthService.sendCode(dto.getPhone());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/phone/verify")
    @Operation(summary = "휴대폰 인증 코드 검증", description = "전송된 휴대폰 인증 코드를 검증합니다.")
    public ResponseEntity<Boolean> verifyPhoneCode(@RequestBody @Valid PhoneVerifyRequestDto dto) {
        boolean isValid = phoneAuthService.verifyCode(dto.getPhone(), dto.getCode());
        return ResponseEntity.ok(isValid);
    }

    @Operation(summary = "JWT 재발급", description = "RefreshToken을 통해 새로운 AccessToken을 발급받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "RefreshToken 유효성 실패"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/token/refresh")
    public ResponseEntity<CommonResponse<TokenResponseDto>> refreshAccessToken(
            @RequestBody @Valid TokenRefreshRequestDto dto,
            HttpServletRequest request
    ) {
        String refreshToken = dto.getRefreshToken();

        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(CommonResponse.fail("Invalid refresh token"));
        }

        String email = jwtTokenProvider.getEmail(refreshToken);
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        AuditRefreshTokenLog logEntry = AuditRefreshTokenLog.of(email, ip, userAgent);
        auditRefreshTokenLogRepository.save(logEntry);
        log.info("✅ Refresh Token 재발급 로그 저장: {}", logEntry);

        String newAccessToken = jwtTokenProvider.createAccessToken(user);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user);
        tokenService.saveRefreshToken(email, newRefreshToken, 1000L * 60 * 60 * 24 * 7);

        return ResponseEntity.ok(CommonResponse.success(new TokenResponseDto(newAccessToken, newRefreshToken)));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "Redis에서 저장된 RefreshToken을 제거합니다.")
    public ResponseEntity<Void> logout(@RequestBody @Valid LogoutRequestDto dto) {
        String refreshToken = dto.getRefreshToken();
        String email = jwtTokenProvider.getEmail(refreshToken);
        tokenService.delete(refreshToken, email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/signup")
    @Operation(summary = "회원 가입", description = "회원 계정을 가입 처리합니다.")
    public ResponseEntity<?> register(@RequestBody @Valid UserSignUpRequestDto dto,
                                      HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        Long userId = userService.register(dto, ip);
        return ResponseEntity.ok().build();
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "회원 탈퇴", description = "비밀번호 확인 및 탈퇴 사유 입력을 통해 회원을 탈퇴 처리합니다.")
    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal User user,
                                         @RequestBody @Valid WithdrawRequestDto dto) {
        try {
            userService.withdraw(user.getId(), dto.getPassword(), dto.getReason());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @PostMapping("/password/reset-request")
    @Operation(summary = "비밀번호 재설정 요청", description = "이메일 주소로 비밀번호 재설정 링크 또는 토큰을 전송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증 이메일 전송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 형식", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    public ResponseEntity<Void> sendResetLink(@RequestBody @Valid PasswordResetRequestDto dto) {
        userService.requestPasswordReset(dto.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/reset")
    @Operation(summary = "비밀번호 재설정", description = "이메일로 받은 토큰을 통해 새 비밀번호로 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 토큰 또는 비밀번호 조건 미달", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid PasswordResetConfirmDto dto) {
        userService.resetPassword(dto.getToken(), dto.getNewPassword());
        return ResponseEntity.ok().build();
    }
}
