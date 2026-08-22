package com.byeolnight.controller.auth;

import com.byeolnight.dto.user.CurrentUserResponseDto;
import com.byeolnight.dto.user.LoginRequestDto;
import com.byeolnight.dto.user.TokenResponseDto;
import com.byeolnight.entity.log.AuditRefreshTokenLog;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.infrastructure.security.JwtTokenProvider;
import com.byeolnight.infrastructure.util.IpUtil;
import com.byeolnight.repository.log.AuditRefreshTokenLogRepository;
import com.byeolnight.service.auth.AuthCookieService;
import com.byeolnight.service.auth.AuthService;
import com.byeolnight.service.auth.TokenService;
import com.byeolnight.service.certificate.CertificateService;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 세션의 생성·갱신·종료와 현재 인증 상태 조회를 담당한다. */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "인증 API")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final AuthCookieService authCookieService;
    private final UserQueryService userQueryService;
    private final CertificateService certificateService;
    private final AuditRefreshTokenLogRepository auditRefreshTokenLogRepository;

    @PostMapping("/login")
    @Operation(summary = "로그인")
    public ResponseEntity<CommonResponse<TokenResponseDto>> login(
            @Valid @RequestBody LoginRequestDto dto, HttpServletRequest request) {
        try {
            AuthService.LoginResult result = authService.authenticate(dto, request);
            ResponseCookie refresh = authCookieService.createRefreshCookie(
                    result.getRefreshToken(), result.getRefreshTokenValidity(), dto.isRememberMe());
            ResponseCookie access = authCookieService.createAccessCookie(
                    result.getAccessToken(), dto.isRememberMe());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refresh.toString())
                    .header(HttpHeaders.SET_COOKIE, access.toString())
                    .body(CommonResponse.success(new TokenResponseDto(result.getAccessToken(), true)));
        } catch (SecurityException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(CommonResponse.fail(exception.getMessage()));
        } catch (BadCredentialsException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.fail(exception.getMessage()));
        } catch (Exception exception) {
            log.error("로그인 처리 오류", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("로그인 처리 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/token/refresh")
    @Operation(summary = "Access Token 재발급")
    public ResponseEntity<CommonResponse<TokenResponseDto>> refreshAccessToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            @RequestHeader(value = "Cookie", required = false) String cookieHeader,
            HttpServletRequest request) {
        try {
            if (refreshToken == null || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.fail("유효하지 않은 Refresh Token"));
            }
            Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
            User user = userQueryService.findById(userId);
            if (user == null) {
                throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
            }
            // 서명만 유효한 탈취·폐기 토큰은 Redis 원본 대조에서 차단한다.
            if (!tokenService.isValidRefreshToken(user.getEmail(), refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.fail("저장된 인증 정보와 일치하지 않는 Refresh Token입니다."));
            }
            auditRefreshTokenLogRepository.save(AuditRefreshTokenLog.of(
                    user.getEmail(), IpUtil.getClientIp(request), request.getHeader("User-Agent")));

            String newAccessToken = jwtTokenProvider.createAccessToken(user);
            String newRefreshToken = jwtTokenProvider.createRefreshToken(user);
            long validity = jwtTokenProvider.getRefreshTokenValidity();
            tokenService.saveRefreshToken(user.getEmail(), newRefreshToken, validity);
            boolean rememberMe = authCookieService.isRememberMeCookie(cookieHeader);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE,
                            authCookieService.createRefreshCookie(newRefreshToken, validity, rememberMe).toString())
                    .header(HttpHeaders.SET_COOKIE,
                            authCookieService.createAccessCookie(newAccessToken, rememberMe).toString())
                    .body(CommonResponse.success(new TokenResponseDto(newAccessToken, true)));
        } catch (IllegalArgumentException exception) {
            log.warn("토큰 재발급 거부: {}", exception.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.fail("인증 정보가 유효하지 않습니다. 다시 로그인해주세요."));
        } catch (Exception exception) {
            log.error("토큰 재발급 오류", exception);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.fail("토큰 재발급에 실패했습니다."));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃")
    public ResponseEntity<CommonResponse<String>> logout(
            @AuthenticationPrincipal User user,
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            @CookieValue(name = "accessToken", required = false) String accessToken,
            HttpServletRequest request) {
        try {
            String email = extractUserEmail(user, accessToken, refreshToken);
            if (email != null) {
                tokenService.deleteRefreshToken(email);
            }
            blacklistToken(accessToken);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, authCookieService.createDeleteCookie("refreshToken").toString())
                    .header(HttpHeaders.SET_COOKIE, authCookieService.createDeleteCookie("accessToken").toString())
                    .header(HttpHeaders.SET_COOKIE, authCookieService.createDeleteCookie("JSESSIONID").toString())
                    .body(CommonResponse.success("로그아웃되었습니다."));
        } catch (Exception exception) {
            log.error("로그아웃 오류", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("로그아웃 처리 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/check")
    @Operation(summary = "인증 상태 확인")
    public ResponseEntity<CommonResponse<String>> checkAuth(@AuthenticationPrincipal User user) {
        return user != null
                ? ResponseEntity.ok(CommonResponse.success("인증됨"))
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(CommonResponse.fail("인증되지 않음"));
    }

    @GetMapping("/me")
    @Operation(summary = "현재 사용자 정보 조회")
    public ResponseEntity<CommonResponse<CurrentUserResponseDto>> getCurrentUser(
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.ok(CommonResponse.success(null));
        }
        try {
            User fullUser = userQueryService.findById(user.getId());
            var certificate = certificateService.getRepresentativeCertificateSafe(fullUser);
            return ResponseEntity.ok(CommonResponse.success(CurrentUserResponseDto.from(fullUser, certificate)));
        } catch (Exception exception) {
            log.error("사용자 정보 조회 실패", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("사용자 정보 조회에 실패했습니다."));
        }
    }

    private String extractUserEmail(User user, String accessToken, String refreshToken) {
        if (user != null) {
            return user.getEmail();
        }
        if (accessToken != null && jwtTokenProvider.validate(accessToken)) {
            return jwtTokenProvider.getEmail(accessToken);
        }
        if (refreshToken != null && jwtTokenProvider.validateRefreshToken(refreshToken)) {
            return jwtTokenProvider.getEmail(refreshToken);
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
