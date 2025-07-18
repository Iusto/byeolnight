package com.byeolnight.controller.auth;

import com.byeolnight.domain.entity.log.AuditRefreshTokenLog;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.log.AuditRefreshTokenLogRepository;
import com.byeolnight.dto.auth.*;
import com.byeolnight.dto.user.*;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.infrastructure.security.JwtTokenProvider;
import com.byeolnight.infrastructure.util.IpUtil;
import com.byeolnight.infrastructure.security.SmsRateLimitService;
import com.byeolnight.infrastructure.security.AuthRateLimitService;
import com.byeolnight.service.auth.AuthService;
import com.byeolnight.service.auth.EmailAuthService;
import com.byeolnight.service.auth.PhoneAuthService;
import com.byeolnight.service.auth.TokenService;
import com.byeolnight.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
@Tag(name = "🔑 인증 API", description = "로그인, 회원가입, 토큰 관리 등 인증 관련 API")
public class AuthController {

    private final AuthService authService;
    private final EmailAuthService emailAuthService;
    private final PhoneAuthService phoneAuthService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final UserService userService;
    private final AuditRefreshTokenLogRepository auditRefreshTokenLogRepository;
    private final com.byeolnight.service.certificate.CertificateService certificateService;
    private final com.byeolnight.service.user.PointService pointService;
    private final com.byeolnight.service.user.MissionService missionService;
    private final SmsRateLimitService smsRateLimitService;
    private final AuthRateLimitService authRateLimitService;

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    public ResponseEntity<CommonResponse<TokenResponseDto>> login(
            @RequestBody(required = true) @Valid Object loginRequest,
            HttpServletRequest request
    ) {
        try {
            // 로그인 요청 데이터 로깅
            log.info("로그인 요청 데이터 형식: {}", loginRequest.getClass().getName());
            
            // 배열 형태로 전송된 경우 처리
            LoginRequestDto dto;
            if (loginRequest instanceof LoginRequestDto) {
                dto = (LoginRequestDto) loginRequest;
            } else if (loginRequest instanceof java.util.List) {
                java.util.List<?> list = (java.util.List<?>) loginRequest;
                if (!list.isEmpty() && list.get(0) instanceof java.util.Map) {
                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) list.get(0);
                    String email = (String) map.get("email");
                    String password = (String) map.get("password");
                    dto = new LoginRequestDto(email, password);
                    log.info("배열 형태의 로그인 요청을 객체로 변환했습니다.");
                } else {
                    return ResponseEntity.badRequest()
                            .body(CommonResponse.fail("잘못된 로그인 요청 형식입니다."));
                }
            } else if (loginRequest instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) loginRequest;
                String email = (String) map.get("email");
                String password = (String) map.get("password");
                dto = new LoginRequestDto(email, password);
                log.info("Map 형태의 로그인 요청을 객체로 변환했습니다.");
            } else {
                return ResponseEntity.badRequest()
                        .body(CommonResponse.fail("잘못된 로그인 요청 형식입니다."));
            }
            
            // 인증 처리를 AuthService에 위임
            AuthService.LoginResult result = authService.authenticate(dto, request);
            
            // HttpOnly 쿠키로 RefreshToken 설정
            ResponseCookie refreshCookie = createRefreshCookie(result.getRefreshToken(), result.getRefreshTokenValidity());
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(CommonResponse.success(new TokenResponseDto(result.getAccessToken())));
                    
        } catch (SecurityException e) {
            log.info("로그인 차단: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(CommonResponse.fail(e.getMessage()));
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            log.info("로그인 인증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("로그인 처리 중 예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("로그인 처리 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/email/send")
    @Operation(summary = "이메일 인증 코드 전송", description = "이메일로 인증 코드를 전송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이메일 전송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "429", description = "요청 제한 초과"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CommonResponse<String>> sendEmailCode(@RequestBody @Valid EmailRequestDto dto, HttpServletRequest request) {
        String clientIp = IpUtil.getClientIp(request);
        
        // Rate Limiting 확인
        if (!authRateLimitService.isEmailAuthAllowed(dto.getEmail(), clientIp)) {
            return ResponseEntity.status(429).body(CommonResponse.fail("이메일 인증 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."));
        }
        
        // 이메일 중복 검사
        if (userService.findByEmail(dto.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.fail("이미 가입된 이메일입니다."));
        }
        
        // 이메일 형식 검증
        if (!isValidEmail(dto.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.fail("올바른 이메일 형식이 아닙니다."));
        }
        
        emailAuthService.sendCode(dto.getEmail());
        return ResponseEntity.ok(CommonResponse.success("이메일 인증 코드가 전송되었습니다."));
    }

    @PostMapping("/email/verify")
    @Operation(summary = "이메일 인증 코드 검증", description = "이메일 인증 코드를 검증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이메일 인증 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 인증 코드"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CommonResponse<Boolean>> verifyEmailCode(@RequestBody @Valid EmailVerifyRequestDto dto) {
        boolean isValid = emailAuthService.verifyCode(dto.getEmail(), dto.getCode());
        return ResponseEntity.ok(CommonResponse.success(isValid));
    }

    @PostMapping("/phone/send")
    @Operation(summary = "전화번호 인증 코드 전송", description = "전화번호로 인증 코드를 전송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "전화번호 인증 코드 전송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "429", description = "요청 제한 초과"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CommonResponse<String>> sendPhoneCode(@RequestBody @Valid PhoneRequestDto dto, HttpServletRequest request) {
        try {
            String clientIp = IpUtil.getClientIp(request);
            
            // Rate Limiting 확인 (기존 SMS + 새로운 통합 제한)
            if (!smsRateLimitService.isSmsAllowed(dto.getPhone(), clientIp) || 
                !authRateLimitService.isSmsAuthAllowed(dto.getPhone(), clientIp)) {
                return ResponseEntity.status(429).body(CommonResponse.fail("SMS 인증 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."));
            }
            
            // 추가 보안: 전화번호 형식 검증
            if (!isValidPhoneNumber(dto.getPhone())) {
                return ResponseEntity.badRequest()
                        .body(CommonResponse.fail("올바른 전화번호 형식이 아닙니다."));
            }
            
            // 핸드폰번호 중복 검사
            if (userService.isPhoneDuplicated(dto.getPhone())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(CommonResponse.fail("이미 가입된 번호입니다."));
            }
            
            phoneAuthService.sendCode(dto.getPhone());
            return ResponseEntity.ok(CommonResponse.success("전화번호 인증 코드가 전송되었습니다."));
        } catch (Exception e) {
            log.error("휴대폰 인증 코드 전송 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("휴대폰 인증 코드 전송에 실패했습니다."));
        }
    }
    
    private boolean isValidPhoneNumber(String phone) {
        // 한국 휴대폰 번호 형식 검증 (010, 011, 016, 017, 018, 019)
        return phone != null && phone.matches("^01[0-9]-?\\d{3,4}-?\\d{4}$");
    }
    
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    @PostMapping("/phone/verify")
    @Operation(summary = "전화번호 인증 코드 검증", description = "전화번호 인증 코드를 검증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "전화번호 인증 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 인증 코드"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CommonResponse<Boolean>> verifyPhoneCode(@RequestBody @Valid PhoneVerifyRequestDto dto) {
        boolean isValid = phoneAuthService.verifyCode(dto.getPhone(), dto.getCode());
        return ResponseEntity.ok(CommonResponse.success(isValid));
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

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(CommonResponse.success(new TokenResponseDto(newAccessToken)));

        } catch (Exception e) {
            log.error("토큰 재발급 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.fail("토큰 재발급에 실패했습니다."));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "사용자 로그아웃을 처리하고, 해당 토큰을 블랙리스트에 추가하고 Refresh Token을 무효화합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CommonResponse<String>> logout(
            @RequestHeader("Authorization") String authHeader,
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        try {
            String accessToken = resolveToken(authHeader);
            String email = jwtTokenProvider.getEmail(accessToken);

            // Access Token 블랙리스트 등록
            long expirationMillis = jwtTokenProvider.getExpiration(accessToken);
            tokenService.blacklistAccessToken(accessToken, expirationMillis);
            log.info("🚫 AccessToken 블랙리스트 등록: {}", accessToken);

            // Refresh Token Redis에서 제거
            if (refreshToken != null && jwtTokenProvider.validateRefreshToken(refreshToken)) {
                tokenService.delete(refreshToken, email);
                log.info("🧹 RefreshToken 삭제 완료: {}", email);
            }

            // 클라이언트에 쿠키 삭제 지시
            ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(0)
                    .build();
            response.setHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

            return ResponseEntity.ok(CommonResponse.success("로그아웃이 완료되었습니다."));
        } catch (Exception e) {
            log.error("로그아웃 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("로그아웃 처리 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새로운 사용자 계정을 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CommonResponse<String>> register(@RequestBody @Valid UserSignUpRequestDto dto, HttpServletRequest request) {
        try {
            String ip = IpUtil.getClientIp(request);
            Long userId = userService.register(dto, ip);
            
            // 회원가입 완료 인증서 발급
            User newUser = userService.findById(userId);
            // 첫 로그인 시 별빛 탐험가 인증서는 로그인 시점에 발급됨
            
            return ResponseEntity.ok(CommonResponse.success("회원가입이 완료되었습니다."));
        } catch (Exception e) {
            log.error("회원가입 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping("/withdraw")
    @Operation(summary = "회원 탈퇴", description = "사용자가 계정을 탈퇴합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CommonResponse<String>> withdraw(@AuthenticationPrincipal User user, @RequestBody @Valid WithdrawRequestDto dto) {
        try {
            if (user == null) {
                log.warn("회원 탈퇴 요청 시 인증되지 않은 사용자");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.fail("인증이 필요합니다. 다시 로그인해주세요."));
            }
            
            userService.withdraw(user.getId(), dto.getPassword(), dto.getReason());
            return ResponseEntity.ok(CommonResponse.success("회원 탈퇴가 완료되었습니다."));
        } catch (Exception e) {
            log.error("회원 탈퇴 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.fail(e.getMessage()));
        }
    }

    @PostMapping("/password/reset-request")
    @Operation(summary = "비밀번호 초기화 요청", description = "비밀번호 초기화 링크를 이메일로 전송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 초기화 요청 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 이메일 주소"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CommonResponse<String>> sendResetLink(@RequestBody @Valid PasswordResetRequestDto dto) {
        try {
            userService.requestPasswordReset(dto.getEmail());
            return ResponseEntity.ok(CommonResponse.success("비밀번호 재설정 링크가 전송되었습니다."));
        } catch (Exception e) {
            log.error("비밀번호 재설정 요청 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.fail(e.getMessage()));
        }
    }

    @PostMapping("/password/reset")
    @Operation(summary = "비밀번호 재설정", description = "새로운 비밀번호로 계정의 비밀번호를 재설정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 재설정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 토큰 또는 비밀번호"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CommonResponse<String>> resetPassword(@RequestBody @Valid PasswordResetConfirmDto dto) {
        try {
            userService.resetPassword(dto.getToken(), dto.getNewPassword());
            return ResponseEntity.ok(CommonResponse.success("비밀번호가 성공적으로 재설정되었습니다."));
        } catch (Exception e) {
            log.error("비밀번호 재설정 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.fail(e.getMessage()));
        }
    }

    @GetMapping("/check-nickname")
    @Operation(summary = "닉네임 중복 체크", description = "사용자 닉네임 중복을 확인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "닉네임 사용 가능 여부"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CommonResponse<Boolean>> checkNickname(@RequestParam("value") String nickname) {
        log.info("[🔍 닉네임 중복 체크 API] 요청 닉네임: '{}'", nickname);
        
        boolean exists = userService.isNicknameDuplicated(nickname);
        boolean available = !exists; // 사용 가능하면 true
        
        log.info("[🔍 닉네임 중복 체크 결과] 닉네임: '{}', 사용가능: {}", nickname, available);
        
        return ResponseEntity.ok(CommonResponse.success(available));
    }

    @PostMapping("/attendance")
    @Operation(summary = "출석 체크", description = "일일 출석 체크를 하고 포인트를 지급받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "출석 체크 성공"),
            @ApiResponse(responseCode = "400", description = "이미 출석함"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CommonResponse<String>> checkAttendance(@AuthenticationPrincipal User user) {
        try {
            boolean success = pointService.checkDailyAttendance(user);
            if (success) {
                // 출석 성공 후 주간 미션 체크
                boolean missionCompleted = missionService.checkWeeklyAttendanceMission(user);
                
                String message = "출석 체크 완료! 스텔라 10개를 획득했습니다.";
                if (missionCompleted) {
                    message += " 추가로 주간 미션을 완료하여 스텔라 50개를 더 획득했습니다!";
                }
                
                return ResponseEntity.ok(CommonResponse.success(message));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(CommonResponse.fail("오늘은 이미 출석하셨습니다."));
            }
        } catch (Exception e) {
            log.error("출석 체크 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("출석 체크 중 오류가 발생했습니다."));
        }
    }

    /**
     * Refresh Token 쿠키 생성
     */
    private ResponseCookie createRefreshCookie(String refreshToken, long validity) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(validity / 1000)
                .build();
    }

    /**
     * Authorization 헤더에서 토큰 추출
     */
    private String resolveToken(String bearer) {
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}