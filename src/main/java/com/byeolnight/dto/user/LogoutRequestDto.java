package com.byeolnight.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LogoutRequestDto {
    @NotBlank
    private String refreshToken;
}