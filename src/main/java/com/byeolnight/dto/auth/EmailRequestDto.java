package com.byeolnight.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "이메일 인증 요청 DTO")
public class EmailRequestDto {
    
    @Email
    @NotBlank
    @Schema(description = "인증할 이메일 주소", example = "user@example.com", required = true)
    private String email;
}