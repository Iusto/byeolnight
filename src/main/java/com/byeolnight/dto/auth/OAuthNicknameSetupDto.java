package com.byeolnight.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthNicknameSetupDto {

    @NotNull
    private Long userId;

    @NotBlank
    @Size(min = 2, max = 8, message = "닉네임은 2-8자로 입력해주세요.")
    private String nickname;
}