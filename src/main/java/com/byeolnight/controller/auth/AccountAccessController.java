package com.byeolnight.controller.auth;

import com.byeolnight.dto.auth.AccountRecoveryTicketRequestDto;
import com.byeolnight.dto.auth.PasswordResetConfirmDto;
import com.byeolnight.dto.auth.PasswordResetRequestDto;
import com.byeolnight.dto.user.TokenResponseDto;
import com.byeolnight.dto.user.WithdrawRequestDto;
import com.byeolnight.entity.token.PasswordResetToken;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.infrastructure.security.JwtTokenProvider;
import com.byeolnight.service.auth.AccountRecoveryService;
import com.byeolnight.service.auth.AccountRecoveryTicketService;
import com.byeolnight.service.auth.AuthCookieService;
import com.byeolnight.service.auth.PasswordResetService;
import com.byeolnight.service.auth.TokenService;
import com.byeolnight.service.user.UserAccountService;
import com.byeolnight.service.user.UserQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 계정 복구·비밀번호 재설정·회원 탈퇴 API를 담당한다. */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "인증 API")
public class AccountAccessController {

    private final PasswordResetService passwordResetService;
    private final UserQueryService userQueryService;
    private final UserAccountService userAccountService;
    private final AccountRecoveryService accountRecoveryService;
    private final AccountRecoveryTicketService accountRecoveryTicketService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final AuthCookieService authCookieService;

    @PostMapping("/account/recover")
    @Operation(summary = "검증된 인증 수단 기반 탈퇴 계정 복구")
    public ResponseEntity<CommonResponse<TokenResponseDto>> handleAccountRecovery(
            @Valid @RequestBody AccountRecoveryTicketRequestDto dto) {
        try {
            AccountRecoveryTicketService.RecoveryIdentity identity =
                    accountRecoveryTicketService.consume(dto.ticket());
            User user = identity.authenticationMethod() == AccountRecoveryTicketService.AuthenticationMethod.OAUTH
                    ? accountRecoveryService.recoverOAuthAccount(
                            identity.userId(), identity.provider(), identity.providerUserId())
                    : accountRecoveryService.recoverPasswordAccount(identity.userId());

            String accessToken = jwtTokenProvider.createAccessToken(user);
            String refreshToken = jwtTokenProvider.createRefreshToken(user);
            long validity = jwtTokenProvider.getRefreshTokenValidity();
            tokenService.saveRefreshToken(user.getEmail(), refreshToken, validity);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE,
                            authCookieService.createRefreshCookie(refreshToken, validity, false).toString())
                    .header(HttpHeaders.SET_COOKIE,
                            authCookieService.createAccessCookie(accessToken, false).toString())
                    .body(CommonResponse.success(new TokenResponseDto(null, true), "계정 복구가 완료되었습니다."));
        } catch (IllegalArgumentException exception) {
            log.warn("계정 복구 거부: {}", exception.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.fail("복구 요청이 만료되었거나 올바르지 않습니다. 다시 로그인해주세요."));
        } catch (Exception exception) {
            log.error("계정 복구 처리 오류", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("처리 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/password/reset-request")
    @Operation(summary = "비밀번호 재설정 요청")
    public ResponseEntity<CommonResponse<String>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDto dto) {
        try {
            passwordResetService.sendPasswordResetEmail(dto.getEmail());
            return ResponseEntity.ok(CommonResponse.success("비밀번호 재설정 이메일을 전송했습니다."));
        } catch (Exception exception) {
            log.error("비밀번호 재설정 요청 실패", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("이메일 전송에 실패했습니다."));
        }
    }

    @GetMapping("/password/validate-token")
    @Operation(summary = "비밀번호 재설정 토큰 검증")
    public ResponseEntity<CommonResponse<String>> validatePasswordResetToken(@RequestParam String token) {
        try {
            PasswordResetToken resetToken = passwordResetService.validateToken(token);
            User user = userQueryService.findByEmail(resetToken.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));
            if (user.isSocialUser()) {
                String provider = user.getSocialProviderName();
                return ResponseEntity.badRequest().body(CommonResponse.fail(
                        String.format("소셜 로그인(%s) 계정입니다. %s에서 비밀번호를 변경해주세요.", provider, provider)));
            }
            return ResponseEntity.ok(CommonResponse.success("유효한 토큰입니다."));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(CommonResponse.fail(exception.getMessage()));
        } catch (Exception exception) {
            log.error("비밀번호 재설정 토큰 검증 실패", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("토큰 검증에 실패했습니다."));
        }
    }

    @PostMapping("/password/reset-confirm")
    @Operation(summary = "비밀번호 재설정 확인")
    public ResponseEntity<CommonResponse<String>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmDto dto) {
        try {
            passwordResetService.resetPassword(dto.getToken(), dto.getNewPassword());
            return ResponseEntity.ok(CommonResponse.success("비밀번호가 성공적으로 변경되었습니다."));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(CommonResponse.fail(exception.getMessage()));
        } catch (Exception exception) {
            log.error("비밀번호 재설정 실패", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("비밀번호 재설정에 실패했습니다."));
        }
    }

    @DeleteMapping("/withdraw")
    @Operation(summary = "회원 탈퇴")
    public ResponseEntity<CommonResponse<String>> withdraw(
            @RequestBody(required = false) WithdrawRequestDto dto,
            @CookieValue(name = "accessToken", required = false) String accessToken,
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletRequest request) {
        try {
            Long userId = extractUserId(accessToken, refreshToken);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.fail("로그인이 필요합니다."));
            }
            User user = userQueryService.findById(userId);
            String password = dto != null && dto.getPassword() != null ? dto.getPassword() : "";
            String reason = dto != null && dto.getReason() != null ? dto.getReason() : "사용자 요청";
            userAccountService.withdraw(user.getId(), password, reason);
            tokenService.deleteRefreshToken(user.getEmail());
            blacklistToken(accessToken);

            ResponseCookie refresh = authCookieService.createDeleteCookie("refreshToken");
            ResponseCookie access = authCookieService.createDeleteCookie("accessToken");
            ResponseCookie session = authCookieService.createDeleteCookie("JSESSIONID");
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refresh.toString())
                    .header(HttpHeaders.SET_COOKIE, access.toString())
                    .header(HttpHeaders.SET_COOKIE, session.toString())
                    .body(CommonResponse.success("회원 탈퇴가 완료되었습니다."));
        } catch (Exception exception) {
            log.error("회원 탈퇴 오류", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail(exception.getMessage()));
        }
    }

    private Long extractUserId(String accessToken, String refreshToken) {
        if (accessToken != null && jwtTokenProvider.validate(accessToken)) {
            return jwtTokenProvider.getUserIdFromToken(accessToken);
        }
        if (refreshToken != null && jwtTokenProvider.validateRefreshToken(refreshToken)) {
            return jwtTokenProvider.getUserIdFromToken(refreshToken);
        }
        return null;
    }

    private void blacklistToken(String token) {
        if (token != null && jwtTokenProvider.validate(token)) {
            long remainingTime = jwtTokenProvider.getExpiration(token);
            if (remainingTime > 0) {
                tokenService.blacklistAccessToken(token, remainingTime);
            }
        }
    }
}
