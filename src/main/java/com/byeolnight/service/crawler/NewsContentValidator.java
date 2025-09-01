package com.byeolnight.service.crawler;

import com.byeolnight.dto.ai.NewsApiResponseDto;
import com.byeolnight.entity.News;
import com.byeolnight.repository.NewsRepository;
import com.byeolnight.infrastructure.config.NewsCollectionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsContentValidator {
    
    private final NewsRepository newsRepository;
    private final NewsCollectionProperties newsConfig;
    
    private static final String[] SPACE_KEYWORDS = {"우주", "로켓", "위성", "화성", "달", "태양", "지구", "목성", "토성", "블랙홀", "은하", "별", "항성", "혜성", "소행성", "망원경", "천문", "항공우주", "우주선", "우주정거장", "우주비행사", "nasa", "spacex", "space", "mars", "moon", "astronomy", "telescope", "satellite", "rocket"};
    private static final String[] EXCLUDE_KEYWORDS = {"trump", "obama", "정치", "선거", "경제", "주식", "코인", "기상", "weather", "날씨", "예보", "팬데믹", "바이러스", "질병"};
    private static final String[] TRUSTED_SOURCES = {"nasa", "esa", "spacex", "science", "nature", "space", "astronomy", "reuters", "ap", "bbc", "cnn", "연합뉴스", "ytn", "kbs", "mbc", "sbs", "한국항공우주연구원", "kari", "과학기술정보통신부"};
    
    public boolean isHighQualityNews(NewsApiResponseDto.Result result) {
        return hasMinimumLength(result) && 
               isSpaceRelated(result) && 
               isReliableSource(result) && 
               !isSimilarToExisting(result);
    }
    
    private boolean hasMinimumLength(NewsApiResponseDto.Result result) {
        String title = result.getTitle() != null ? result.getTitle() : "";
        String description = result.getDescription() != null ? result.getDescription() : "";
        
        return title.length() >= 20 && description.length() >= 50;
    }
    
    private boolean isSpaceRelated(NewsApiResponseDto.Result result) {
        String content = (result.getTitle() + " " + result.getDescription()).toLowerCase();
        
        // 비우주 키워드 체크
        for (String exclude : EXCLUDE_KEYWORDS) {
            if (content.contains(exclude)) return false;
        }
        
        // 우주 키워드 체크 (기준 완화: 3개 → 1개)
        int keywordCount = 0;
        for (String keyword : SPACE_KEYWORDS) {
            if (content.contains(keyword.toLowerCase())) keywordCount++;
        }
        
        return keywordCount >= 1;
    }
    
    private boolean isReliableSource(NewsApiResponseDto.Result result) {
        String sourceName = result.getSourceName();
        if (sourceName == null) return true; // null인 경우 통과 (다른 조건으로 필터링)
        
        String sourceNameLower = sourceName.toLowerCase();
        
        // 신뢰할 수 있는 출처 체크
        for (String trusted : TRUSTED_SOURCES) {
            if (sourceNameLower.contains(trusted)) return true;
        }
        
        // 의심스러운 출처만 제외 (나머지는 통과)
        String[] untrusted = {"blog", "personal", "unknown", "anonymous", "fake", "rumor"};
        for (String pattern : untrusted) {
            if (sourceNameLower.contains(pattern)) return false;
        }
        
        return true; // 기본적으로 통과
    }
    
    private boolean isSimilarToExisting(NewsApiResponseDto.Result result) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        List<News> recentNews = newsRepository.findByPublishedAtAfter(cutoffDate);
        
        String newTitle = normalizeTitle(result.getTitle());
        
        return recentNews.stream()
                .anyMatch(news -> calculateSimilarity(newTitle, normalizeTitle(news.getTitle())) > 0.7);
    }
    
    private String normalizeTitle(String title) {
        if (title == null) return "";
        return title.toLowerCase()
                .replaceAll("[^\\w\\s가-힣]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
    
    private double calculateSimilarity(String title1, String title2) {
        String[] words1 = title1.split("\\s+");
        String[] words2 = title2.split("\\s+");
        
        int commonWords = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2) && word1.length() > 2) {
                    commonWords++;
                    break;
                }
            }
        }
        
        return (double) commonWords / Math.max(words1.length, words2.length);
    }
}