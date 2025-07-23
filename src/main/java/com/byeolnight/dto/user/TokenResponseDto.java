package com.byeolnight.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "JWT 토큰 응답 DTO")
public class TokenResponseDto {

    @Schema(description = "Access Token (HttpOnly 쿠키로 전달됨)", example = "null")
    private String accessToken;
    
    @Schema(description = "로그인 성공 여부", example = "true")
    private boolean success = true;
}
