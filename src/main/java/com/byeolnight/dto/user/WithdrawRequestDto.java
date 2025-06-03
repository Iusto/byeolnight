package com.byeolnight.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class WithdrawRequestDto {

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

    private String reason;
}
