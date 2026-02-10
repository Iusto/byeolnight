package com.byeolnight.dto.external.weather;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OpenWeatherClouds {
    private Double all;

    public double getCloudCover() {
        return all != null ? all : 50.0;
    }
}
