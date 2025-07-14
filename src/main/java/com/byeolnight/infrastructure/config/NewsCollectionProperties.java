package com.byeolnight.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "news")
public class NewsCollectionProperties {
    
    private Collection collection = new Collection();
    private Quality quality = new Quality();
    
    @Data
    public static class Collection {
        private int maxPosts = 1;
        private double similarityThreshold = 0.7;
        private int similarityCheckDays = 7;
    }
    
    @Data
    public static class Quality {
        private int minTitleLength = 15;
        private int minDescriptionLength = 80;
        private int minSpaceKeywords = 2;
        private int maxHashtags = 8;
    }
}