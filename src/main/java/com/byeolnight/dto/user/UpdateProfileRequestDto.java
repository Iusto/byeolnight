package com.byeolnight.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequestDto {

    @NotBlank
    private String nickname;

    @NotBlank
    private String phone;

    @NotBlank
    private String currentPassword;
}