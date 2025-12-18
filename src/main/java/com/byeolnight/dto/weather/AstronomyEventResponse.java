package com.byeolnight.dto.weather;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AstronomyEventResponse {
    private Long id;
    private String eventType;
    private String title;
    private String description;
    private String eventDate;
    private String peakTime;
    private String visibility;
    private String magnitude;
    private Boolean isActive;
}