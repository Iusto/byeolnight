package com.byeolnight.domain.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherResponse {
    private String location;
    private Double latitude;
    private Double longitude;
    private Double cloudCover;
    private Double visibility;
    private String moonPhase;
    private String observationQuality;
    private String recommendation;
    private String observationTime;
}