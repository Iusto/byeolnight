package com.byeolnight.dto.external.vision;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class VisionRequest {
    private List<AnnotateImageRequest> requests;

    public static VisionRequest safeSearchDetection(String base64Image) {
        Image image = new Image(base64Image);
        Feature feature = new Feature("SAFE_SEARCH_DETECTION", 1);
        AnnotateImageRequest request = new AnnotateImageRequest(image, List.of(feature));
        return new VisionRequest(List.of(request));
    }

    @Getter
    @AllArgsConstructor
    public static class AnnotateImageRequest {
        private Image image;
        private List<Feature> features;
    }

    @Getter
    @AllArgsConstructor
    public static class Image {
        private String content;
    }

    @Getter
    @AllArgsConstructor
    public static class Feature {
        private String type;
        private int maxResults;
    }
}
