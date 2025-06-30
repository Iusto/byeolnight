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
    @Pattern(regexp = "^[가-힣a-zA-Z]+$", message = "닉네임은 한글 또는 영어만 가능합니다.")
    private String nickname;

    @NotBlank(message = "현재 비밀번호를 입력해주세요.")
    private String currentPassword;
}