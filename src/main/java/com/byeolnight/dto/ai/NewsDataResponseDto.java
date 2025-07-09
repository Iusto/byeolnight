package com.byeolnight.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class NewsDataResponseDto {
    
    private String status;
    private int totalResults;
    private List<Result> results;
    
    @Data
    public static class Result {
        @JsonProperty("article_id")
        private String articleId;
        private String title;
        private String link;
        private String[] keywords;
        private String[] creator;
        @JsonProperty("video_url")
        private String videoUrl;
        private String description;
        private String content;
        @JsonProperty("pubDate")
        private String pubDate;
        @JsonProperty("image_url")
        private String imageUrl;
        @JsonProperty("source_id")
        private String sourceId;
        @JsonProperty("source_priority")
        private Integer sourcePriority;
        private String country;
        private String[] category;
        private String language;
    }
}