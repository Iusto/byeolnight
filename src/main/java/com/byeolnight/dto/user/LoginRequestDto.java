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

    @Schema(description = "로그인 유지 여부", example = "false")
    private final boolean rememberMe;

    @Builder
    @JsonCreator
    public LoginRequestDto(
            @JsonProperty("email") String email, 
            @JsonProperty("password") String password,
            @JsonProperty("rememberMe") Boolean rememberMe) {
        this.email = email;
        this.password = password;
        this.rememberMe = rememberMe != null ? rememberMe : false;
    }
}
