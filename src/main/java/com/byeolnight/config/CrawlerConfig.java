package com.byeolnight.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CrawlerConfig {
    
    @Value("${crawler.news.api.key:byeolnight-crawler-news}")
    private String newsApiKey;

    public boolean isValidNewsApiKey(String apiKey) {
        return newsApiKey.equals(apiKey);
    }
    
    // 하위 호환성을 위한 통합 검증 메서드
    public boolean isValidApiKey(String apiKey) {
        return isValidNewsApiKey(apiKey);
    }
    
    public String getNewsApiKey() {
        return newsApiKey;
    }
}