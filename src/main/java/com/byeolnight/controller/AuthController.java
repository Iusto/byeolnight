package com.byeolnight.controller;

import com.byeolnight.application.auth.EmailAuthService;
import com.byeolnight.application.auth.PhoneAuthService;
import com.byeolnight.application.auth.TokenService;
import com.byeolnight.application.user.UserService;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.dto.auth.*;
import com.byeolnight.dto.user.LoginRequestDto;
import com.byeolnight.dto.user.LogoutRequestDto;
import com.byeolnight.dto.user.TokenResponseDto;
import com.byeolnight.infrastructure.security.TokenProvider;
import com.byeolnight.infrastructure.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final TokenProvider tokenProvider;
    private final EmailAuthService emailAuthService;
    private final PhoneAuthService phoneAuthService;
    private final TokenService tokenService;

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "로그인 실패"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<TokenResponseDto>> login(@RequestBody @Valid LoginRequestDto dto) {
        User user = userService.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String accessToken = tokenProvider.createAccessToken(user);
        String refreshToken = tokenProvider.createRefreshToken(user);
        tokenService.saveRefreshToken(user.getEmail(), refreshToken, 1000L * 60 * 60 * 24 * 7);

        return ResponseEntity.ok(CommonResponse.success(new TokenResponseDto(accessToken, refreshToken)));
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
    public ResponseEntity<CommonResponse<TokenResponseDto>> refreshAccessToken(@RequestBody @Valid TokenRefreshRequestDto dto) {
        String refreshToken = dto.getRefreshToken();

        if (!tokenProvider.validateRefreshToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(CommonResponse.fail("Invalid refresh token"));
        }

        String email = tokenProvider.getEmail(refreshToken);
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String newAccessToken = tokenProvider.createAccessToken(user);
        String newRefreshToken = tokenProvider.createRefreshToken(user);
        tokenService.saveRefreshToken(email, newRefreshToken, 1000L * 60 * 60 * 24 * 7);

        return ResponseEntity.ok(CommonResponse.success(new TokenResponseDto(newAccessToken, newRefreshToken)));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "Redis에서 저장된 RefreshToken을 제거합니다.")
    public ResponseEntity<Void> logout(@RequestBody @Valid LogoutRequestDto dto) {
        String refreshToken = dto.getRefreshToken();
        String email = tokenProvider.getEmail(refreshToken);
        tokenService.delete(refreshToken, email);
        return ResponseEntity.ok().build();
    }
}