package com.byeolnight.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cinema")
public class CinemaCollectionProperties {
    
    private Collection collection = new Collection();
    private Quality quality = new Quality();
    private Youtube youtube = new Youtube();
    
    @Data
    public static class Collection {
        private int maxPosts = 1;                    // 최대 저장 개수
        private int similarityCheckDays = 30;       // 유사도 체크 기간 (일)
        private double similarityThreshold = 0.7;   // 유사도 임계값
        private int keywordCount = 3;               // 사용할 키워드 개수
        private int retryCount = 3;                 // 재시도 횟수
    }
    
    @Data
    public static class Quality {
        private int minTitleLength = 10;            // 최소 제목 길이
        private int minDescriptionLength = 50;      // 최소 설명 길이
        private int maxResults = 20;                // YouTube API 결과 개수
        private String videoDuration = "medium";    // 비디오 길이 (short, medium, long)
        private String videoDefinition = "high";    // 비디오 화질
    }
    
    @Data
    public static class Youtube {
        private String[] qualityChannels = {
            "NASA", "SpaceX", "ESA", "National Geographic", 
            "Discovery", "Science Channel", "Kurzgesagt"
        };
        private String[] professionalTerms = {
            "documentary", "science", "research", "mission", 
            "exploration", "universe", "cosmos"
        };
        private int publishedAfterYears = 2;        // 최근 N년 영상만
    }
}