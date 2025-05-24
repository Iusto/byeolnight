package com.byeolnight.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UserLoginRequestDto {

    @Email
    @NotBlank
    private final String email;

    @NotBlank
    private final String password;

    @Builder
    public UserLoginRequestDto(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
