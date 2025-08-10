package com.byeolnight.infrastructure.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 별빛시네마 영상 수집 시스템 설정
 * 
 * 역할:
 * - YouTube API 기반 우주 영상 수집 설정
 * - AI 기반 영상 번역 및 품질 기준 정의
 * - 고품질 우주 다큐멘터리 필터링 설정
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Component
@ConfigurationProperties(prefix = "cinema")
public class CinemaCollectionProperties extends BaseCollectionProperties {
    
    private Youtube youtube = new Youtube();
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Collection extends BaseCollectionProperties.Collection {
        private int similarityCheckDays = 30;       // 영화는 더 긴 기간 체크
        private int keywordCount = 3;               // 사용할 키워드 개수
        private int retryCount = 3;                 // 재시도 횟수
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Quality extends BaseCollectionProperties.Quality {
        private int maxResults = 20;                // YouTube API 결과 개수
        private String videoDuration = "medium";    // 비디오 길이 (short/medium/long)
        private String videoDefinition = "high";    // 비디오 화질 (any/standard/high)
        private long minViewCount = 10000;          // 최소 조회수 (1만회)
        private long minLikeCount = 100;            // 최소 좋아요 수
        private double minEngagementRate = 0.01;    // 최소 참여율 (좋아요/조회수)
    }
    
    @Data
    public static class Youtube {
        private String[] qualityChannels = {
            "NASA", "SpaceX", "ESA", "European Space Agency",
            "National Geographic", "Discovery", "Science Channel", 
            "Kurzgesagt", "Veritasium", "SciShow Space",
            "Fraser Cain", "Isaac Arthur", "Anton Petrov",
            "SEA", "Cool Worlds", "Launch Pad Astronomy",
            "한국항공우주연구원", "KARI", "우주항공청"
        };
        private String[] professionalTerms = {
            "documentary", "science", "research", "mission", 
            "exploration", "universe", "cosmos", "astrophysics",
            "cosmology", "observatory", "telescope", "probe"
        };
        private int publishedAfterYears = 2;        // 최근 N년 영상만
        private String[] preferredLanguages = {"ko", "en"};  // 선호 언어
        private String[] regionCodes = {"KR", "US", "GB"};   // 지역 코드
    }
    
    // 영화 전용 인스턴스로 오버라이드
    private Collection collection = new Collection();
    private Quality quality = new Quality();
}