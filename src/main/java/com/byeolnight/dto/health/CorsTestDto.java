package com.byeolnight.dto.health;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorsTestDto {
    private String cors;
    private Boolean allowCredentials;
    private String allowedOrigins;
    private String allowedMethods;
    private LocalDateTime timestamp;
}
