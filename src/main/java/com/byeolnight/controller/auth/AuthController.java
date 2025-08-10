package com.byeolnight.controller.auth;

import com.byeolnight.dto.user.LoginRequestDto;
import com.byeolnight.dto.user.TokenResponseDto;
import com.byeolnight.entity.log.AuditRefreshTokenLog;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.log.AuditRefreshTokenLogRepository;
import com.byeolnight.dto.user.UserSignUpRequestDto;
import com.byeolnight.dto.auth.*;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.infrastructure.security.JwtTokenProvider;
import com.byeolnight.infrastructure.util.IpUtil;
import com.byeolnight.service.auth.AuthService;
import com.byeolnight.service.auth.TokenService;
import com.byeolnight.service.auth.EmailAuthService;
import com.byeolnight.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
@Tag(name = "인증 API")
public class AuthController {
    
    @Value("${app.security.cookie.domain:}")
    private String cookieDomain;

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final UserService userService;
    private final EmailAuthService emailAuthService;
    private final AuditRefreshTokenLogRepository auditRefreshTokenLogRepository;

    @PostMapping("/login")
    @Operation(summary = "로그인")
    public ResponseEntity<CommonResponse<TokenResponseDto>> login(
            @Valid @RequestBody LoginRequestDto dto, HttpServletRequest request) {
        try {
            AuthService.LoginResult result = authService.authenticate(dto, request);
            ResponseCookie refreshCookie = createRefreshCookie(result.getRefreshToken(), result.getRefreshTokenValidity());
            ResponseCookie accessCookie = createAccessCookie(result.getAccessToken());
            TokenResponseDto tokenResponse = new TokenResponseDto(result.getAccessToken(), true);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .body(CommonResponse.success(tokenResponse));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(CommonResponse.fail(e.getMessage()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(CommonResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("로그인 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("로그인 처리 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/token/refresh")
    @Operation(summary = "Access Token 재발급")
    public ResponseEntity<CommonResponse<TokenResponseDto>> refreshAccessToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletRequest request) {
        try {
            if (refreshToken == null || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.fail("유효하지 않은 Refresh Token"));
            }

            String email = jwtTokenProvider.getEmail(refreshToken);
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

            auditRefreshTokenLogRepository.save(AuditRefreshTokenLog.of(email, 
                    IpUtil.getClientIp(request), request.getHeader("User-Agent")));

            String newAccessToken = jwtTokenProvider.createAccessToken(user);
            String newRefreshToken = jwtTokenProvider.createRefreshToken(user);
            long refreshTokenValidity = jwtTokenProvider.getRefreshTokenValidity();

            tokenService.saveRefreshToken(email, newRefreshToken, refreshTokenValidity);

            ResponseCookie refreshCookie = createRefreshCookie(newRefreshToken, refreshTokenValidity);
            ResponseCookie accessCookie = createAccessCookie(newAccessToken);
            TokenResponseDto tokenResponse = new TokenResponseDto(newAccessToken, true);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .body(CommonResponse.success(tokenResponse));

        } catch (Exception e) {
            log.error("토큰 재발급 오류", e);
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
            String userEmail = extractUserEmail(user, accessToken, refreshToken);
            
            if (userEmail != null) {
                tokenService.deleteRefreshToken(userEmail);
            }
            
            blacklistToken(accessToken);
            blacklistAuthHeaderToken(request, accessToken);
            
            ResponseCookie deleteRefreshCookie = createDeleteCookie("refreshToken");
            ResponseCookie deleteAccessCookie = createDeleteCookie("accessToken");
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, deleteRefreshCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, deleteAccessCookie.toString())
                    .body(CommonResponse.success("로그아웃되었습니다."));
                    
        } catch (Exception e) {
            log.error("로그아웃 오류", e);
            return ResponseEntity.ok().body(CommonResponse.success("로그아웃되었습니다."));
        }
    }

    @PostMapping("/email/send")
    @Operation(summary = "이메일 인증 코드 전송")
    public ResponseEntity<CommonResponse<String>> sendEmailCode(@Valid @RequestBody EmailRequestDto dto) {
        try {
            emailAuthService.sendCode(dto.getEmail());
            return ResponseEntity.ok(CommonResponse.success("이메일 인증 코드가 전송되었습니다."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(CommonResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("이메일 인증 코드 전송 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("이메일 전송에 실패했습니다."));
        }
    }

    @PostMapping("/email/verify")
    @Operation(summary = "이메일 인증 코드 확인")
    public ResponseEntity<CommonResponse<Boolean>> verifyEmailCode(@Valid @RequestBody EmailVerifyRequestDto dto) {
        try {
            boolean verified = emailAuthService.verifyCode(dto.getEmail(), dto.getCode());
            return ResponseEntity.ok(CommonResponse.success(verified));
        } catch (Exception e) {
            log.error("이메일 인증 코드 확인 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("이메일 인증에 실패했습니다."));
        }
    }
    
    @DeleteMapping("/email/cleanup")
    @Operation(summary = "이메일 인증 데이터 정리 (페이지 이탈 시)")
    public ResponseEntity<CommonResponse<String>> cleanupEmailData(@Valid @RequestBody EmailRequestDto dto) {
        try {
            emailAuthService.clearAllEmailData(dto.getEmail());
            return ResponseEntity.ok(CommonResponse.success("이메일 인증 데이터가 정리되었습니다."));
        } catch (Exception e) {
            log.error("이메일 인증 데이터 정리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("데이터 정리에 실패했습니다."));
        }
    }
    
    @GetMapping("/email/status")
    @Operation(summary = "이메일 인증 상태 확인")
    public ResponseEntity<CommonResponse<Boolean>> checkEmailStatus(@RequestParam String email) {
        try {
            boolean verified = emailAuthService.isAlreadyVerified(email);
            return ResponseEntity.ok(CommonResponse.success(verified));
        } catch (Exception e) {
            log.error("이메일 인증 상태 확인 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("인증 상태 확인에 실패했습니다."));
        }
    }

    @GetMapping("/check-nickname")
    @Operation(summary = "닉네임 중복 확인")
    public ResponseEntity<CommonResponse<Boolean>> checkNickname(@RequestParam String value) {
        try {
            boolean available = !userService.existsByNickname(value);
            return ResponseEntity.ok(CommonResponse.success(available));
        } catch (Exception e) {
            log.error("닉네임 중복 확인 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("닉네임 확인에 실패했습니다."));
        }
    }

    @PostMapping("/signup")
    @Operation(summary = "회원가입")
    public ResponseEntity<CommonResponse<String>> signup(
            @Valid @RequestBody UserSignUpRequestDto dto, HttpServletRequest request) {
        try {
            if (!emailAuthService.isAlreadyVerified(dto.getEmail())) {
                return ResponseEntity.badRequest().body(CommonResponse.fail("이메일 인증이 필요합니다."));
            }

            userService.createUser(dto, request);
            emailAuthService.clearAllEmailData(dto.getEmail());
            
            return ResponseEntity.ok(CommonResponse.success("회원가입이 완료되었습니다."));
        } catch (Exception e) {
            log.error("회원가입 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail(e.getMessage()));
        }
    }

    private ResponseCookie createRefreshCookie(String refreshToken, long validity) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(validity / 1000);
        
        if (!cookieDomain.isEmpty()) {
            builder.domain(cookieDomain);
        }
        
        return builder.build();
    }

    private ResponseCookie createAccessCookie(String accessToken) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(1800);
        
        if (!cookieDomain.isEmpty()) {
            builder.domain(cookieDomain);
        }
        
        return builder.build();
    }

    private ResponseCookie createDeleteCookie(String name) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0);
        
        if (!cookieDomain.isEmpty()) {
            builder.domain(cookieDomain);
        }
        
        return builder.build();
    }

    private String extractUserEmail(User user, String accessToken, String refreshToken) {
        if (user != null) return user.getEmail();
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

    private void blacklistAuthHeaderToken(HttpServletRequest request, String accessToken) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String headerToken = authHeader.substring(7);
            if (!headerToken.equals(accessToken)) {
                blacklistToken(headerToken);
            }
        }
    }
}