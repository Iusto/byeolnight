package com.byeolnight.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PhoneVerifyRequestDto {
    @NotBlank
    private String phone;

    @NotBlank
    private String code;
}