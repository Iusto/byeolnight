package com.byeolnight.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class NewsApiResponseDto {
    
    private String status;
    private int totalResults;
    private String nextPage;
    private List<Result> results;
    
    @Data
    public static class Result {
        @JsonProperty("article_id")
        private String articleId;
        
        private String title;
        private String link;
        private List<String> keywords;
        private List<String> creator;
        private String description;
        private String content;
        
        @JsonProperty("pubDate")
        private String pubDate;
        
        @JsonProperty("image_url")
        private String imageUrl;
        
        @JsonProperty("source_id")
        private String sourceId;
        
        @JsonProperty("source_name")
        private String sourceName;
        
        private List<String> country;
        private List<String> category;
        private String language;
    }
}