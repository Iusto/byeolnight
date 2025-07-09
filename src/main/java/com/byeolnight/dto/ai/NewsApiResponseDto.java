package com.byeolnight.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class NewsApiResponseDto {
    
    private String status;
    private int totalResults;
    private List<Article> articles;
    
    @Data
    public static class Article {
        private Source source;
        private String author;
        private String title;
        private String description;
        private String url;
        private String urlToImage;
        private String publishedAt;
        private String content;
    }
    
    @Data
    public static class Source {
        private String id;
        private String name;
    }
}