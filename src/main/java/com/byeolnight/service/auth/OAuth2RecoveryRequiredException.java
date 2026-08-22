package com.byeolnight.service.auth;

import lombok.Getter;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

@Getter
public class OAuth2RecoveryRequiredException extends OAuth2AuthenticationException {

    private static final String ERROR_CODE = "oauth_account_recovery_required";

    private final String recoveryTicket;

    public OAuth2RecoveryRequiredException(String recoveryTicket) {
        super(new OAuth2Error(ERROR_CODE), "탈퇴 계정 복구 확인이 필요합니다.");
        this.recoveryTicket = recoveryTicket;
    }
}
