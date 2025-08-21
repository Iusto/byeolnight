package com.byeolnight.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PasswordResetConfirmDto {
    @NotBlank
    private String token;

    @NotBlank
    private String newPassword;
}

