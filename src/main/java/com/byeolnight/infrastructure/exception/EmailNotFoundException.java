package com.byeolnight.infrastructure.exception;

/**
 * 비밀번호 재설정 요청 시 이메일이 존재하지 않을 경우 발생
 */
public class EmailNotFoundException extends RuntimeException {
    public EmailNotFoundException(String message) {
        super(message);
    }
}
