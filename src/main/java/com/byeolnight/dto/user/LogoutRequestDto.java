package com.byeolnight.dto.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LogoutRequestDto {
    @NotBlank
    private String refreshToken;

    @JsonCreator
    public LogoutRequestDto(@JsonProperty("refreshToken") String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
