package com.byeolnight.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequestDto {

    @NotBlank(message = "닉네임을 입력해주세요.")
    @Size(min = 2, max = 8, message = "닉네임은 2-8자로 입력해주세요.")
    private String nickname;

    private String currentPassword; // 소셜 로그인 사용자는 선택사항
}