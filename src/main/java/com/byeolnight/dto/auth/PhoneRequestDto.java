package com.byeolnight.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PhoneRequestDto {
    @NotBlank
    private String phone;
}