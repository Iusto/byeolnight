package com.byeolnight.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "JWT 토큰 응답 DTO")
public class TokenResponseDto {

    @Schema(description = "Access Token (30분 유효)", example = "eyJhbGciOiJIUzI1...")
    private String accessToken;
}
