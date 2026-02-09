package com.byeolnight.dto.weather;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssObservationResponse {
    private String messageKey;
    private String friendlyMessage;
    private Double currentAltitudeKm;
    private Double currentVelocityKmh;
    private String nextPassTime;
    private String nextPassDate;
    private String nextPassDirection;
    private String estimatedDuration;
    private String visibilityQuality;
    private Double maxElevation;
}
