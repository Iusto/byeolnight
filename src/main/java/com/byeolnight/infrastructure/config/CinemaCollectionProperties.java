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
        private String videoDuration = "medium";    // 비디오 길이
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
    
    // 영화 전용 인스턴스로 오버라이드
    private Collection collection = new Collection();
    private Quality quality = new Quality();
}