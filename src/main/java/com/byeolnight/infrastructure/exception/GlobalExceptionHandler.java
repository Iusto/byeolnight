package com.byeolnight.infrastructure.exception;

import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.infrastructure.exception.DuplicateEmailException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 유효성 검사 실패 (DTO validation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<?>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldError() != null ?
                ex.getBindingResult().getFieldError().getDefaultMessage() : "잘못된 요청입니다.";
        return ResponseEntity.badRequest().body(CommonResponse.fail(message));
    }

    /**
     * 단일 파라미터 유효성 검사 실패 (e.g., @RequestParam)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CommonResponse<?>> handleConstraint(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(CommonResponse.fail("유효하지 않은 입력입니다."));
    }

    /**
     * 이메일 중복 예외
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<CommonResponse<?>> handleDuplicateEmail(DuplicateEmailException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CommonResponse.fail(ex.getMessage()));
    }

    /**
     * 닉네임 중복 예외
     */
    @ExceptionHandler(DuplicateNicknameException.class)
    public ResponseEntity<CommonResponse<?>> handleDuplicateNickname(DuplicateNicknameException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(CommonResponse.fail("이미 사용 중인 닉네임입니다."));
    }

    /**
     * 이메일 없음 예외
     */
    @ExceptionHandler(EmailNotFoundException.class)
    public ResponseEntity<CommonResponse<?>> handleEmailNotFound(EmailNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(CommonResponse.fail("등록되지 않은 이메일입니다."));
    }

    /**
     * 비밀번호 불일치 예외
     */
    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<CommonResponse<?>> handlePasswordMismatch(PasswordMismatchException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(CommonResponse.fail("비밀번호가 일치하지 않습니다."));
    }

    /**
     * 잘못된 요청 예외
     */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<CommonResponse<?>> handleInvalidRequest(InvalidRequestException ex) {
        return ResponseEntity.badRequest().body(CommonResponse.fail(ex.getMessage()));
    }

    /**
     * 리소스 없음 예외
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<CommonResponse<?>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(CommonResponse.fail(ex.getMessage()));
    }

    /**
     * 잘못된 인수 예외
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonResponse<?>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(CommonResponse.fail(ex.getMessage()));
    }

    /**
     * 만료된 비밀번호 초기화 토큰 예외
     */
    @ExceptionHandler(ExpiredResetTokenException.class)
    public ResponseEntity<CommonResponse<?>> handleExpiredResetToken(ExpiredResetTokenException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CommonResponse.fail("비밀번호 초기화 토큰이 만료되었습니다."));
    }

    /**
     * 건의사항 찾을 수 없음 예외
     */
    @ExceptionHandler(SuggestionNotFoundException.class)
    public ResponseEntity<CommonResponse<?>> handleSuggestionNotFound(SuggestionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(CommonResponse.fail(ex.getMessage()));
    }

    /**
     * 건의사항 접근 권한 없음 예외
     */
    @ExceptionHandler(SuggestionAccessDeniedException.class)
    public ResponseEntity<CommonResponse<?>> handleSuggestionAccessDenied(SuggestionAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(CommonResponse.fail(ex.getMessage()));
    }

    /**
     * 건의사항 수정 불가 예외
     */
    @ExceptionHandler(SuggestionModificationException.class)
    public ResponseEntity<CommonResponse<?>> handleSuggestionModification(SuggestionModificationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CommonResponse.fail(ex.getMessage()));
    }

    /**
     * 멀티파트 요청 예외 처리
     */
    @ExceptionHandler(org.springframework.web.multipart.MultipartException.class)
    public ResponseEntity<CommonResponse<?>> handleMultipartException(org.springframework.web.multipart.MultipartException ex) {
        log.warn("[멀티파트 요청 오류]", ex);
        return ResponseEntity.badRequest()
                .body(CommonResponse.fail("파일 업로드 요청이 올바르지 않습니다. multipart/form-data 형식으로 요청해주세요."));
    }

    /**
     * JSON 파싱 오류 처리
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<CommonResponse<?>> handleHttpMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("[JSON 파싱 오류]", ex);
        return ResponseEntity.badRequest()
                .body(CommonResponse.fail("요청 형식이 올바르지 않습니다. JSON 형식을 확인해주세요."));
    }

    /**
     * HTTP 메시지 변환 오류 처리 (Content-Type 불일치)
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotWritableException.class)
    public ResponseEntity<CommonResponse<?>> handleHttpMessageNotWritable(org.springframework.http.converter.HttpMessageNotWritableException ex) {
        log.warn("[HTTP 메시지 변환 오류] Content-Type: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(CommonResponse.fail("지원하지 않는 콘텐츠 타입입니다. application/json을 사용해주세요."));
    }

    /**
     * OAuth2 리소스 찾을 수 없음 예외 처리
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<CommonResponse<?>> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        String path = ex.getResourcePath();
        if (path != null && path.contains("oauth2/authorization")) {
            log.warn("[OAuth2 인증 오류] OAuth2 설정을 확인해주세요: {}", path);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(CommonResponse.fail("소셜 로그인 서비스가 일시적으로 사용할 수 없습니다."));
        }
        log.warn("[리소스 없음] {}", path);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CommonResponse.fail("요청한 리소스를 찾을 수 없습니다."));
    }

    /**
     * 그 외 알 수 없는 서버 에러
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<?>> handleException(Exception ex) {
        log.error("[서버 에러]", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommonResponse.fail("서버 오류가 발생했습니다."));
    }
}