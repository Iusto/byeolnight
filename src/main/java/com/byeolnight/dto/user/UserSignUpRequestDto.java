package com.byeolnight.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UserSignUpRequestDto {

    @Email
    @NotBlank
    private final String email;

    @NotBlank
    private final String password;

    @NotBlank
    private final String nickname;

    @NotBlank
    private final String phone;

    @Builder
    public UserSignUpRequestDto(String email, String password, String nickname, String phone) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.phone = phone;
    }
}
