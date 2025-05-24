package com.byeolnight.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class EmailRequestDto {
    @Email
    @NotBlank
    private String email;
}