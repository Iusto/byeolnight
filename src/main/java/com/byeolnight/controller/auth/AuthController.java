package com.byeolnight.controller.auth;

import com.byeolnight.domain.entity.log.AuditLoginLog;
import com.byeolnight.domain.entity.log.AuditRefreshTokenLog;
import com.byeolnight.domain.entity.log.AuditSignupLog;
import com.byeolnight.domain.repository.log.AuditLoginLogRepository;
import com.byeolnight.domain.repository.log.AuditRefreshTokenLogRepository;
import com.byeolnight.domain.repository.log.AuditSignupLogRepository;
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
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
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
    private final AuditSignupLogRepository auditSignupLogRepository;
    private final AuditLoginLogRepository auditLoginLogRepository;
    private final StringRedisTemplate redisTemplate;

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    public ResponseEntity<CommonResponse<TokenResponseDto>> login(
            @RequestBody @Valid LoginRequestDto dto,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            String ip = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            // ✅ [1] 사전 보안 검사: 차단된 IP인지 확인
            if (Boolean.TRUE.equals(redisTemplate.hasKey("blocked:ip:" + ip))) {
                log.warn("🚫 차단된 IP 로그인 시도: {}", ip);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(CommonResponse.fail("해당 IP는 비정상적인 로그인 시도로 인해 차단되었습니다."));
            }

            // ✅ [2] 사용자 존재 여부 확인
            User user = userService.findByEmail(dto.getEmail())
                    .orElseThrow(() -> {
                        // 로그인 시도에 사용된 이메일이 존재하지 않으면 로그인 실패 기록
                        auditSignupLogRepository.save(AuditSignupLog.failure(dto.getEmail(), ip, "존재하지 않는 이메일"));
                        return new IllegalArgumentException("존재하지 않는 사용자입니다.");
                    });

            // ✅ [3] 계정 상태 확인 (탈퇴, 정지, 밴 등)
            if (user.getStatus() != User.UserStatus.ACTIVE) {
                auditSignupLogRepository.save(AuditSignupLog.failure(user.getEmail(), ip, "비활성 상태: " + user.getStatus()));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.fail("해당 계정은 로그인할 수 없습니다. 현재 상태: " + user.getStatus()));
            }

            // ✅ [4] 계정이 잠겨 있는 경우 로그인 차단
            if (user.isAccountLocked()) {
                auditSignupLogRepository.save(AuditSignupLog.failure(user.getEmail(), ip, "계정 잠김 상태"));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.fail("계정이 잠겨 있습니다. 관리자에게 문의하세요."));
            }

            // ✅ [5] 비밀번호 검증 실패 시 로그인 실패 처리 및 실패 횟수 증가
            if (!userService.checkPassword(dto.getPassword(), user)) {
                userService.increaseLoginFailCount(user, ip, userAgent);

                // 로그인 실패 10회 도달 시 계정 잠금 안내 메시지 반환
                if (user.getLoginFailCount() == 10) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(CommonResponse.fail("비밀번호가 10회 이상 틀렸습니다. 계정이 잠겼습니다. 비밀번호를 초기화해야 잠금이 해제됩니다."));
                }

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.fail("비밀번호가 일치하지 않습니다."));
            }

            // ✅ [6] 로그인 성공 처리 (실패 횟수 초기화, 마지막 로그인 기록 등)
            userService.resetLoginFailCount(user);

            // ✅ [7] 성공 로그 기록
            auditLoginLogRepository.save(AuditLoginLog.of(user.getEmail(), ip, userAgent));

            // ✅ [8] Access & Refresh Token 발급
            String accessToken = jwtTokenProvider.createAccessToken(user);
            String refreshToken = jwtTokenProvider.createRefreshToken(user);
            long refreshTokenValidity = jwtTokenProvider.getRefreshTokenValidity();

            // ✅ [9] Refresh Token을 Redis에 저장
            tokenService.saveRefreshToken(user.getEmail(), refreshToken, refreshTokenValidity);

            // ✅ [10] HttpOnly 쿠키로 RefreshToken 클라이언트에 전달
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(refreshTokenValidity / 1000)
                    .build();

            // ✅ [11] AccessToken은 JSON으로 응답
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(CommonResponse.success(new TokenResponseDto(accessToken)));

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
    @Operation(summary = "Access Token 재발급", description = "HttpOnly 쿠키에 저장된 Refresh Token을 사용하여 새로운 Access Token을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access Token 재발급 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Refresh Token이 유효하지 않음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CommonResponse<TokenResponseDto>> refreshAccessToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            // 1. Refresh Token 존재 및 유효성 검증
            if (refreshToken == null || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(CommonResponse.fail("유효하지 않은 Refresh Token"));
            }

            // 2. 이메일 추출 + 유저 조회
            String email = jwtTokenProvider.getEmail(refreshToken);
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

            // 3. 로그 기록
            String ip = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            AuditRefreshTokenLog logEntry = AuditRefreshTokenLog.of(email, ip, userAgent);
            auditRefreshTokenLogRepository.save(logEntry);
            log.info("✅ Refresh Token 재발급 로그 저장: {}", logEntry);

            // 4. 새 토큰 생성
            String newAccessToken = jwtTokenProvider.createAccessToken(user);
            String newRefreshToken = jwtTokenProvider.createRefreshToken(user);
            long refreshTokenValidity = jwtTokenProvider.getRefreshTokenValidity();

            // 5. Redis 갱신 (Rotation)
            tokenService.saveRefreshToken(email, newRefreshToken, refreshTokenValidity);

            // 6. 새 Refresh Token을 HttpOnly 쿠키로 전달
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", newRefreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(refreshTokenValidity / 1000)
                    .build();
            response.setHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            // 7. Access Token만 응답 본문에 포함
            return ResponseEntity.ok(CommonResponse.success(new TokenResponseDto(newAccessToken)));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("서버 오류가 발생했습니다."));
        }
    }


    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "사용자 로그아웃을 처리하고, 해당 토큰을 블랙리스트에 추가하고 Refresh Token을 무효화합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authHeader,
            @CookieValue(name = "refreshToken", required = false) String refreshToken, // 👈 쿠키에서 추출
            HttpServletResponse response
    ) {
        String accessToken = resolveToken(authHeader);
        String email = jwtTokenProvider.getEmail(accessToken); // 이메일 추출

        // 1. Access Token 블랙리스트 등록
        long expirationMillis = jwtTokenProvider.getExpiration(accessToken);
        tokenService.blacklistAccessToken(accessToken, expirationMillis);
        log.info("🚫 AccessToken 블랙리스트 등록: {}", accessToken);

        // 2. Refresh Token Redis에서 제거
        if (refreshToken != null && jwtTokenProvider.validateRefreshToken(refreshToken)) {
            tokenService.delete(refreshToken, email); // Redis에서 refresh:email 키 삭제
            log.info("🧹 RefreshToken 삭제 완료: {}", email);
        } else {
            log.warn("⚠️ 유효하지 않은 Refresh Token 또는 쿠키 없음");
        }

        // 3. 클라이언트에 쿠키 삭제 지시
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0) // ⏱️ 쿠키 만료
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

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