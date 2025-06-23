package com.byeolnight.controller;

import com.byeolnight.domain.entity.log.AuditRefreshTokenLog;
import com.byeolnight.domain.repository.AuditRefreshTokenLogRepository;
import com.byeolnight.dto.user.*;
import com.byeolnight.infrastructure.security.JwtTokenProvider;
import com.byeolnight.service.auth.EmailAuthService;
import com.byeolnight.service.auth.PhoneAuthService;
import com.byeolnight.service.auth.TokenService;
import com.byeolnight.service.user.UserService;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.dto.auth.*;
import com.byeolnight.infrastructure.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

            if (user.getStatus() != User.UserStatus.ACTIVE) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.fail("해당 계정은 로그인할 수 없습니다. 현재 상태: " + user.getStatus()));
            }

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
    @Operation(summary = "이메일 인증 코드 전송", description = "이메일로 인증 코드를 전송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이메일 전송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> sendEmailCode(@RequestBody @Valid EmailRequestDto dto) {
        emailAuthService.sendCode(dto.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/verify")
    @Operation(summary = "이메일 인증 코드 검증", description = "이메일 인증 코드를 검증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이메일 인증 성공", content = @Content(schema = @Schema(type = "boolean"))),
            @ApiResponse(responseCode = "400", description = "잘못된 인증 코드"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Boolean> verifyEmailCode(@RequestBody @Valid EmailVerifyRequestDto dto) {
        boolean isValid = emailAuthService.verifyCode(dto.getEmail(), dto.getCode());
        return ResponseEntity.ok(isValid);
    }

    @PostMapping("/phone/send")
    @Operation(summary = "전화번호 인증 코드 전송", description = "전화번호로 인증 코드를 전송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "전화번호 인증 코드 전송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> sendPhoneCode(@RequestBody @Valid PhoneRequestDto dto) {
        phoneAuthService.sendCode(dto.getPhone());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/phone/verify")
    @Operation(summary = "전화번호 인증 코드 검증", description = "전화번호 인증 코드를 검증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "전화번호 인증 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 인증 코드"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
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

    @PostMapping("/token/refresh")
    @Operation(summary = "Refresh Token 재발급", description = "Refresh Token을 사용하여 새로운 Access Token을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refresh Token 재발급 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "잘못된 Refresh Token"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CommonResponse<TokenResponseDto>> refreshAccessToken(
            @RequestBody @Valid TokenRefreshRequestDto dto,
            HttpServletRequest request
    ) {
        String refreshToken = dto.getRefreshToken();

        if (!jwtTokenProvider.validate(refreshToken)) {
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
    @Operation(summary = "로그아웃", description = "사용자 로그아웃을 처리하고, 해당 토큰을 블랙리스트에 추가합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String accessToken = resolveToken(authHeader);
        long expirationMillis = jwtTokenProvider.getExpiration(accessToken);
        tokenService.blacklistAccessToken(accessToken, expirationMillis);
        log.info("🚫 AccessToken 블랙리스트 등록: {}", accessToken);
        return ResponseEntity.ok().build();
    }

    private String resolveToken(String bearer) {
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새로운 사용자 계정을 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> register(@RequestBody @Valid UserSignUpRequestDto dto, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        Long userId = userService.register(dto, ip);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/withdraw")
    @Operation(summary = "회원 탈퇴", description = "사용자가 계정을 탈퇴합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal User user, @RequestBody @Valid WithdrawRequestDto dto) {
        userService.withdraw(user.getId(), dto.getPassword(), dto.getReason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/reset-request")
    @Operation(summary = "비밀번호 초기화 요청", description = "비밀번호 초기화 링크를 이메일로 전송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 초기화 요청 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 이메일 주소"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> sendResetLink(@RequestBody @Valid PasswordResetRequestDto dto) {
        userService.requestPasswordReset(dto.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/reset")
    @Operation(summary = "비밀번호 재설정", description = "새로운 비밀번호로 계정의 비밀번호를 재설정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 재설정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 토큰 또는 비밀번호"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid PasswordResetConfirmDto dto) {
        userService.resetPassword(dto.getToken(), dto.getNewPassword());
        return ResponseEntity.ok().build();
    }


    @GetMapping("/check-nickname")
    @Operation(summary = "닉네임 중복 체크", description = "사용자 닉네임 중복을 확인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "닉네임 사용 가능 여부", content = @Content(schema = @Schema(type = "boolean"))),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Boolean> checkNickname(@RequestParam("value") String nickname) {
        boolean exists = userService.isNicknameDuplicated(nickname);
        return ResponseEntity.ok(exists);
    }
}