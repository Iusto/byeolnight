package com.byeolnight.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AstronomyCollectionStatusDto {
    private String scheduledCollection;
    private String manualCollection;
    private Map<String, String> dataSources;
    private Map<String, String> supportedEventTypes;
}
