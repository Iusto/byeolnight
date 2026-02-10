package com.byeolnight.dto.external.vision;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class VisionResponse {
    private List<AnnotateImageResponse> responses;

    public SafeSearchAnnotation getFirstSafeSearchAnnotation() {
        if (responses != null && !responses.isEmpty()) {
            return responses.get(0).getSafeSearchAnnotation();
        }
        return null;
    }

    @Getter
    @NoArgsConstructor
    public static class AnnotateImageResponse {
        private SafeSearchAnnotation safeSearchAnnotation;
    }

    @Getter
    @NoArgsConstructor
    public static class SafeSearchAnnotation {
        private String adult;
        private String violence;
        private String racy;
        private String spoof;
        private String medical;

        public String getAdultOrDefault() {
            return adult != null ? adult : "UNKNOWN";
        }

        public String getViolenceOrDefault() {
            return violence != null ? violence : "UNKNOWN";
        }

        public String getRacyOrDefault() {
            return racy != null ? racy : "UNKNOWN";
        }

        public String getSpoofOrDefault() {
            return spoof != null ? spoof : "UNKNOWN";
        }

        public String getMedicalOrDefault() {
            return medical != null ? medical : "UNKNOWN";
        }
    }
}
