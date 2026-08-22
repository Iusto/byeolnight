package com.byeolnight.dto.weather;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class WeatherResponse {
    private String location;
    private Double latitude;
    private Double longitude;
    private Double cloudCover;
    private Double visibility;
    private String moonPhase;
    private Integer observationScore;
    private Integer cloudScore;
    private Integer visibilityScore;
    private Integer moonScore;
    private String observationQuality;
    private String recommendation;
    private String observationTime;
    private DataStatus dataStatus;
    private String lastSuccessfulAt;

    public enum DataStatus {
        FRESH,
        STALE,
        UNAVAILABLE
    }
}
