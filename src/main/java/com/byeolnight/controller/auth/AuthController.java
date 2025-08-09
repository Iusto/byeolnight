package com.byeolnight.controller.auth;

import com.byeolnight.entity.log.AuditRefreshTokenLog;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.log.AuditRefreshTokenLogRepository;
import com.byeolnight.dto.user.*;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.infrastructure.security.JwtTokenProvider;
import com.byeolnight.infrastructure.util.IpUtil;
import com.byeolnight.service.auth.AuthService;
import com.byeolnight.service.auth.TokenService;
import com.byeolnight.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "🔑 인증 API", description = "로그인, 회원가입, 토큰 관리 등 인증 관련 API")
public class AuthController {
    
    @Value("${app.security.cookie.secure:false}")
    private boolean secureCookie;
    
    @Value("${app.security.cookie.domain:}")
    private String cookieDomain;

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final UserService userService;
    private final AuditRefreshTokenLogRepository auditRefreshTokenLogRepository;

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    public ResponseEntity<CommonResponse<TokenResponseDto>> login(
            @Valid @RequestBody LoginRequestDto dto,
            HttpServletRequest request
    ) {
        try {
            // 요청 본문 로깅 제거 (ContentCachingFilter에서 처리)
            AuthService.LoginResult result = authService.authenticate(dto, request);

            // Refresh Token 쿠키 설정
            ResponseCookie refreshCookie = createRefreshCookie(result.getRefreshToken(), result.getRefreshTokenValidity());
            
            // Access Token도 HttpOnly 쿠키로 설정
            ResponseCookie.ResponseCookieBuilder accessCookieBuilder = ResponseCookie.from("accessToken", result.getAccessToken())
                    .httpOnly(true)
                    .secure(secureCookie)
                    .sameSite("None")
                    .path("/")
                    .maxAge(1800);
            
            if (!cookieDomain.isEmpty()) {
                accessCookieBuilder.domain(cookieDomain);
            }
            
            ResponseCookie accessCookie = accessCookieBuilder.build();

            // 토큰을 응답 본문에 명시적으로 포함 (중요: 프론트엔드에서 사용)
            TokenResponseDto tokenResponse = new TokenResponseDto(result.getAccessToken(), true);
            log.info("로그인 성공: 토큰을 응답 본문에 포함 (길이: {})", result.getAccessToken().length());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .body(CommonResponse.success(tokenResponse));
        } catch (SecurityException e) {
            log.info("로그인 차단: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(CommonResponse.fail(e.getMessage()));
        } catch (BadCredentialsException e) {
            log.info("로그인 인증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(CommonResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("로그인 처리 중 예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("로그인 처리 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/token/refresh")
    @Operation(summary = "Access Token 재발급", description = "HttpOnly 쿠키에 저장된 Refresh Token을 사용하여 새로운 Access Token을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access Token 재발급 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Refresh Token이 유효하지 않음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CommonResponse<TokenResponseDto>> refreshAccessToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletRequest request
    ) {
        try {
            // Refresh Token 존재 및 유효성 검증
            if (refreshToken == null || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.fail("유효하지 않은 Refresh Token"));
            }

            // 이메일 추출 및 사용자 조회
            String email = jwtTokenProvider.getEmail(refreshToken);
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

            // 로그 기록
            String ip = IpUtil.getClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            AuditRefreshTokenLog logEntry = AuditRefreshTokenLog.of(email, ip, userAgent);
            auditRefreshTokenLogRepository.save(logEntry);
            log.info("✅ Refresh Token 재발급 로그 저장: {}", logEntry);

            // 새 토큰 생성
            String newAccessToken = jwtTokenProvider.createAccessToken(user);
            String newRefreshToken = jwtTokenProvider.createRefreshToken(user);
            long refreshTokenValidity = jwtTokenProvider.getRefreshTokenValidity();

            // Redis 갱신 (Token Rotation)
            tokenService.saveRefreshToken(email, newRefreshToken, refreshTokenValidity);

            // 새 Refresh Token을 HttpOnly 쿠키로 전달
            ResponseCookie refreshCookie = createRefreshCookie(newRefreshToken, refreshTokenValidity);
            
            // Access Token도 HttpOnly 쿠키로 전달
            ResponseCookie.ResponseCookieBuilder accessCookieBuilder = ResponseCookie.from("accessToken", newAccessToken)
                    .httpOnly(true)
                    .secure(secureCookie)
                    .sameSite("None")
                    .path("/")
                    .maxAge(1800);
            
            if (!cookieDomain.isEmpty()) {
                accessCookieBuilder.domain(cookieDomain);
            }
            
            ResponseCookie accessCookie = accessCookieBuilder.build();

            // 토큰을 응답 본문에 명시적으로 포함 (중요: 프론트엔드에서 사용)
            TokenResponseDto tokenResponse = new TokenResponseDto(newAccessToken, true);
            log.info("토큰 갱신 성공: 토큰을 응답 본문에 포함 (길이: {})", newAccessToken.length());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .body(CommonResponse.success(tokenResponse));

        } catch (Exception e) {
            log.error("토큰 재발급 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.fail("토큰 재발급에 실패했습니다."));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 사용자를 로그아웃하고 토큰을 무효화합니다.")
    public ResponseEntity<CommonResponse<String>> logout(
            @AuthenticationPrincipal User user,
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            @CookieValue(name = "accessToken", required = false) String accessToken,
            HttpServletRequest request
    ) {
        log.info("🚪 로그아웃 API 호출됨 - 사용자: {}, 쿠키 토큰 존재: {}", 
                user != null ? user.getEmail() : "null", accessToken != null);
        try {
            String userEmail = null;
            
            // 사용자 정보가 있으면 Refresh Token 삭제
            if (user != null) {
                userEmail = user.getEmail();
                tokenService.deleteRefreshToken(userEmail);
            }
            
            // 쿠키의 Access Token 블랙리스트 등록 (사용자 인증 여부와 무관)
            if (accessToken != null && jwtTokenProvider.validate(accessToken)) {
                long remainingTime = jwtTokenProvider.getExpiration(accessToken);
                if (remainingTime > 0) {
                    tokenService.blacklistAccessToken(accessToken, remainingTime);
                    if (userEmail == null) {
                        userEmail = jwtTokenProvider.getEmail(accessToken);
                    }
                    log.info("🚫 쿠키 Access Token 블랙리스트 등록: 사용자 {}, 남은 시간 {}ms", userEmail, remainingTime);
                }
            }
            
            // Authorization 헤더의 토큰도 블랙리스트 등록
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String headerToken = authHeader.substring(7);
                if (!headerToken.equals(accessToken) && jwtTokenProvider.validate(headerToken)) {
                    long remainingTime = jwtTokenProvider.getExpiration(headerToken);
                    if (remainingTime > 0) {
                        tokenService.blacklistAccessToken(headerToken, remainingTime);
                        if (userEmail == null) {
                            userEmail = jwtTokenProvider.getEmail(headerToken);
                        }
                        log.info("🚫 Authorization 헤더 토큰 블랙리스트 등록: 사용자 {}", userEmail);
                    }
                }
            }
            
            // Refresh Token도 있으면 삭제 (사용자 정보 없어도 토큰에서 이메일 추출)
            if (refreshToken != null && jwtTokenProvider.validateRefreshToken(refreshToken)) {
                if (userEmail == null) {
                    userEmail = jwtTokenProvider.getEmail(refreshToken);
                }
                tokenService.deleteRefreshToken(userEmail);
                log.info("🗑️ Refresh Token 삭제: 사용자 {}", userEmail);
            }
            
            log.info("로그아웃 성공: 사용자 {} 토큰 무효화 완료", userEmail != null ? userEmail : "unknown");
            
            // 쿠키 삭제
            ResponseCookie.ResponseCookieBuilder deleteRefreshBuilder = ResponseCookie.from("refreshToken", "")
                    .httpOnly(true)
                    .secure(secureCookie)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(0);
            
            ResponseCookie.ResponseCookieBuilder deleteAccessBuilder = ResponseCookie.from("accessToken", "")
                    .httpOnly(true)
                    .secure(secureCookie)
                    .sameSite("None")
                    .path("/")
                    .maxAge(0);
            
            if (!cookieDomain.isEmpty()) {
                deleteRefreshBuilder.domain(cookieDomain);
                deleteAccessBuilder.domain(cookieDomain);
            }
            
            ResponseCookie deleteRefreshCookie = deleteRefreshBuilder.build();
            ResponseCookie deleteAccessCookie = deleteAccessBuilder.build();
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, deleteRefreshCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, deleteAccessCookie.toString())
                    .body(CommonResponse.success("로그아웃되었습니다."));
                    
        } catch (Exception e) {
            log.error("로그아웃 처리 중 오류 발생", e);
            // 오류가 발생해도 로그아웃은 성공으로 처리 (보안상 이유)
            return ResponseEntity.ok()
                    .body(CommonResponse.success("로그아웃되었습니다."));
        }
    }

    /**
     * Refresh Token 쿠키 생성
     */
    private ResponseCookie createRefreshCookie(String refreshToken, long validity) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("None")
                .path("/")
                .maxAge(validity / 1000);
        
        if (!cookieDomain.isEmpty()) {
            builder.domain(cookieDomain);
        }
        
        return builder.build();
    }
}