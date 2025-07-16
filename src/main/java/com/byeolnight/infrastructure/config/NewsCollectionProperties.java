package com.byeolnight.infrastructure.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 뉴스 수집 시스템 설정
 * 
 * 역할:
 * - NewsData.io API 기반 뉴스 수집 설정
 * - AI 기반 뉴스 처리 품질 기준 정의
 * - 중복 뉴스 필터링 및 유사도 검사 설정
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Component
@ConfigurationProperties(prefix = "news")
public class NewsCollectionProperties extends BaseCollectionProperties {
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Quality extends BaseCollectionProperties.Quality {
        private int minTitleLength = 15;            // 뉴스 제목 최소 길이
        private int minDescriptionLength = 80;      // 뉴스 설명 최소 길이
        private int minSpaceKeywords = 2;           // 우주 키워드 최소 개수
        private int maxHashtags = 8;                // 최대 해시태그 개수
    }
    
    // 뉴스 전용 Quality 인스턴스로 오버라이드
    private Quality quality = new Quality();
}