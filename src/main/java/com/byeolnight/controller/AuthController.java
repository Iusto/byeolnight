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

import java.util.HashMap;
import java.util.Map;

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

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "로그인 실패"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CommonResponse<TokenResponseDto>> login(@RequestBody @Valid LoginRequestDto dto) {
        try {
            User user = userService.findByEmail(dto.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

            // ✅ 계정 상태 확인
            if (user.getStatus() != User.UserStatus.ACTIVE) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.fail("해당 계정은 로그인할 수 없습니다. 현재 상태: " + user.getStatus()));
            }

            // ✅ 계정 잠금 여부 확인
            if (user.isAccountLocked()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.fail("계정이 잠겨 있습니다. 관리자에게 문의하세요."));
            }

            // ✅ 비밀번호 확인
            if (!userService.checkPassword(dto.getPassword(), user)) {
                userService.increaseLoginFailCount(user);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.fail("비밀번호가 일치하지 않습니다."));
            }

            // ✅ 로그인 성공 처리
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
    @Operation(summary = "이메일 인증 코드 검증", description = "전송된 이메일 인증 코드를 검증합니다.")
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
    public ResponseEntity<Map<String, Object>> verifyPhoneCode(@RequestBody @Valid PhoneVerifyRequestDto dto) {
        Map<String, Object> response = new HashMap<>();
        String phone = dto.getPhone();
        String code = dto.getCode();

        if (phoneAuthService.isAlreadyVerified(phone)) {
            response.put("success", false);
            response.put("message", "이미 인증된 전화번호입니다.");
            return ResponseEntity.ok(response);
        }

        boolean isValid = phoneAuthService.verifyCode(phone, code);
        if (isValid) {
            response.put("success", true);
            response.put("message", "인증에 성공했습니다.");
        } else {
            response.put("success", false);
            response.put("message", "인증 코드가 일치하지 않습니다.");
        }

        return ResponseEntity.ok(response);
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
    @Operation(summary = "로그아웃", description = "AccessToken을 블랙리스트에 등록합니다.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authHeader
    ) {
        String accessToken = resolveToken(authHeader);

        // accessToken 만료 시간 계산
        long expirationMillis = jwtTokenProvider.getExpiration(accessToken);

        // accessToken 블랙리스트 등록
        tokenService.blacklistAccessToken(accessToken, expirationMillis);
        log.info("🚫 AccessToken 블랙리스트 등록: {}", accessToken);

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

    /**
     * Authorization 헤더에서 Bearer AccessToken 추출
     */
    private String resolveToken(String bearer) {
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    /**
     * 닉네임 중복 확인 API
     * - 회원가입 및 프로필 수정 시 닉네임 사용 가능 여부 확인 용도
     * - 로그인 필요 없음
     *
     * 예: GET /api/auth/check-nickname?value=Jade99
     * 응답: true (중복됨), false (사용 가능)
     */
    @Operation(
            summary = "닉네임 중복 확인",
            description = "회원가입 또는 프로필 수정 시 입력한 닉네임이 이미 사용 중인지 확인합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "중복 여부 반환 성공",
                    content = @Content(schema = @Schema(implementation = Boolean.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 형식", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping("/check-nickname")
    public ResponseEntity<Boolean> checkNickname(@RequestParam("value") String nickname) {
        boolean exists = userService.isNicknameDuplicated(nickname);
        return ResponseEntity.ok(exists);
    }

}
