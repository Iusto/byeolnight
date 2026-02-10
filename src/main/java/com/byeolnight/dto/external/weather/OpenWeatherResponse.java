package com.byeolnight.dto.external.weather;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OpenWeatherResponse {
    private String name;
    private OpenWeatherClouds clouds;
    private Integer visibility;

    public double getCloudCover() {
        return clouds != null ? clouds.getCloudCover() : 50.0;
    }

    public double getVisibilityKm() {
        return visibility != null ? visibility / 1000.0 : 10.0;
    }

    public String getLocationName() {
        return (name != null && !name.isEmpty()) ? name : "알 수 없음";
    }
}
