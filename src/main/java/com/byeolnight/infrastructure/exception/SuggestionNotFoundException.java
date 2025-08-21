package com.byeolnight.infrastructure.exception;

public class SuggestionNotFoundException extends RuntimeException {
    public SuggestionNotFoundException(String message) {
        super(message);
    }
    
    public SuggestionNotFoundException() {
        super("건의사항을 찾을 수 없습니다.");
    }
}