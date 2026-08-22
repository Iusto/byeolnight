package com.byeolnight.controller.auth;

import com.byeolnight.dto.auth.EmailRequestDto;
import com.byeolnight.dto.auth.EmailVerifyRequestDto;
import com.byeolnight.dto.user.UserSignUpRequestDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.infrastructure.util.IpUtil;
import com.byeolnight.service.auth.EmailAuthService;
import com.byeolnight.service.user.UserAccountService;
import com.byeolnight.service.user.UserQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 이메일 인증과 일반 회원가입 API만 담당한다. */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "인증 API")
public class RegistrationController {

    private final EmailAuthService emailAuthService;
    private final UserAccountService userAccountService;
    private final UserQueryService userQueryService;

    @PostMapping("/email/send")
    @Operation(summary = "이메일 인증 코드 전송")
    public ResponseEntity<CommonResponse<String>> sendEmailCode(@Valid @RequestBody EmailRequestDto dto) {
        try {
            emailAuthService.sendCode(dto.getEmail());
            return ResponseEntity.ok(CommonResponse.success("이메일 인증 코드를 전송했습니다."));
        } catch (IllegalStateException exception) {
            return ResponseEntity.badRequest().body(CommonResponse.fail(exception.getMessage()));
        } catch (Exception exception) {
            log.error("이메일 인증 코드 전송 실패", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("이메일 전송에 실패했습니다."));
        }
    }

    @PostMapping("/email/verify")
    @Operation(summary = "이메일 인증 코드 확인")
    public ResponseEntity<CommonResponse<Boolean>> verifyEmailCode(
            @Valid @RequestBody EmailVerifyRequestDto dto, HttpServletRequest request) {
        try {
            boolean verified = emailAuthService.verifyCode(
                    dto.getEmail(), dto.getCode(), IpUtil.getClientIp(request));
            return ResponseEntity.ok(CommonResponse.success(verified));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(CommonResponse.fail(exception.getMessage()));
        } catch (Exception exception) {
            log.error("이메일 인증 코드 확인 실패", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("이메일 인증에 실패했습니다."));
        }
    }

    @DeleteMapping("/email/cleanup")
    @Operation(summary = "이메일 인증 임시 데이터 정리")
    public ResponseEntity<CommonResponse<String>> cleanupEmailData(@Valid @RequestBody EmailRequestDto dto) {
        emailAuthService.clearAllEmailData(dto.getEmail());
        return ResponseEntity.ok(CommonResponse.success("이메일 인증 데이터를 정리했습니다."));
    }

    @GetMapping("/email/status")
    @Operation(summary = "이메일 인증 상태 확인")
    public ResponseEntity<CommonResponse<Boolean>> checkEmailStatus(@RequestParam String email) {
        return ResponseEntity.ok(CommonResponse.success(emailAuthService.isAlreadyVerified(email)));
    }

    @GetMapping("/check-nickname")
    @Operation(summary = "닉네임 중복 확인")
    public ResponseEntity<CommonResponse<Boolean>> checkNickname(@RequestParam String value) {
        return ResponseEntity.ok(CommonResponse.success(!userQueryService.existsByNickname(value)));
    }

    @PostMapping("/signup")
    @Operation(summary = "회원가입")
    public ResponseEntity<CommonResponse<String>> signup(
            @Valid @RequestBody UserSignUpRequestDto dto, HttpServletRequest request) {
        try {
            if (!emailAuthService.isAlreadyVerified(dto.getEmail())) {
                return ResponseEntity.badRequest().body(CommonResponse.fail("이메일 인증이 필요합니다."));
            }
            userAccountService.register(dto, IpUtil.getClientIp(request));
            emailAuthService.clearVerificationStatus(dto.getEmail());
            return ResponseEntity.ok(CommonResponse.success("회원가입이 완료되었습니다."));
        } catch (Exception exception) {
            log.error("회원가입 실패", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail(exception.getMessage()));
        }
    }
}
