package com.byeolnight.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AstronomyStatsDto {
    private Long totalCount;
    private String lastUpdated;
    private String error;
}
