package com.byeolnight.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AstronomyCollectionResultDto {
    private boolean success;
    private String message;
    private Integer afterCount;
    private String error;
}
