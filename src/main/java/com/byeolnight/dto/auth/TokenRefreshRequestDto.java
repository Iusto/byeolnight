package com.byeolnight.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class TokenRefreshRequestDto {
    @NotBlank
    private String refreshToken;
}