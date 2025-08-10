package com.byeolnight.dto.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(description = "로그인 요청 DTO")
public class LoginRequestDto {

    @Email
    @NotBlank
    @Schema(description = "사용자 이메일", example = "user@example.com", required = true)
    private final String email;

    @NotBlank
    @Schema(description = "비밀번호", example = "password123!", required = true)
    private final String password;

    @Builder
    @JsonCreator
    public LoginRequestDto(
            @JsonProperty("email") String email, 
            @JsonProperty("password") String password) {
        this.email = email;
        this.password = password;
    }
}
