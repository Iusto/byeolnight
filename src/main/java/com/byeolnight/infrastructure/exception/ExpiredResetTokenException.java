package com.byeolnight.infrastructure.exception;

/**
 * 만료되었거나 이미 사용된 토큰
 */
public class ExpiredResetTokenException extends RuntimeException {
    public ExpiredResetTokenException(String message) {
        super(message);
    }
}
