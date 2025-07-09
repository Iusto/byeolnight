package com.byeolnight.service.crawler;

import com.byeolnight.domain.entity.News;
import com.byeolnight.domain.repository.NewsRepository;
import com.byeolnight.dto.ai.NewsDataResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpaceNewsService {
    
    private final NewsRepository newsRepository;
    private final NewsApiClient newsApiClient;
    
    @Transactional
    public void collectAndSaveSpaceNews() {
        log.info("우주 뉴스 수집 시작");
        
        NewsDataResponseDto response = newsApiClient.fetchSpaceNews();
        if (response == null || response.getResults() == null) {
            log.warn("뉴스 데이터를 가져올 수 없습니다");
            return;
        }
        
        List<News> savedNews = new ArrayList<>();
        int duplicateCount = 0;
        
        for (NewsDataResponseDto.Result result : response.getResults()) {
            if (isDuplicateNews(result)) {
                duplicateCount++;
                log.debug("중복 뉴스 스킵: {}", result.getTitle());
                continue;
            }
            
            News news = convertToNews(result);
            News saved = newsRepository.save(news);
            savedNews.add(saved);
            log.info("새 뉴스 저장: {}", saved.getTitle());
        }
        
        log.info("우주 뉴스 수집 완료 - 저장: {}건, 중복 스킵: {}건", savedNews.size(), duplicateCount);
    }
    
    private boolean isDuplicateNews(NewsDataResponseDto.Result result) {
        return newsRepository.existsByTitle(result.getTitle()) || 
               newsRepository.existsByUrl(result.getLink());
    }
    
    private News convertToNews(NewsDataResponseDto.Result result) {
        return News.builder()
                .title(result.getTitle())
                .description(result.getDescription())
                .imageUrl(result.getImageUrl() != null ? result.getImageUrl() : getDefaultSpaceImage())
                .url(result.getLink())
                .publishedAt(parsePublishedAt(result.getPubDate()))
                .hashtags(generateHashtags(result.getTitle(), result.getDescription()))
                .source(result.getSourceId() != null ? result.getSourceId() : "Unknown")
                .summary("") // 현재는 비워둠
                .build();
    }
    
    private LocalDateTime parsePublishedAt(String publishedAt) {
        try {
            return LocalDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.warn("발행일 파싱 실패: {}, 현재 시간으로 설정", publishedAt);
            return LocalDateTime.now();
        }
    }
    
    private String generateHashtags(String title, String description) {
        List<String> tags = new ArrayList<>();
        
        String content = (title + " " + (description != null ? description : "")).toLowerCase();
        
        if (content.contains("우주") || content.contains("space")) tags.add("#우주");
        if (content.contains("nasa")) tags.add("#NASA");
        if (content.contains("천문") || content.contains("astronomy")) tags.add("#천문학");
        if (content.contains("로켓") || content.contains("rocket")) tags.add("#로켓");
        if (content.contains("위성") || content.contains("satellite")) tags.add("#위성");
        if (content.contains("화성") || content.contains("mars")) tags.add("#화성");
        if (content.contains("달") || content.contains("moon")) tags.add("#달");
        if (content.contains("태양") || content.contains("sun")) tags.add("#태양");
        if (content.contains("행성") || content.contains("planet")) tags.add("#행성");
        if (content.contains("은하") || content.contains("galaxy")) tags.add("#은하");
        
        return String.join(" ", tags);
    }
    
    private String getDefaultSpaceImage() {
        return "https://images.unsplash.com/photo-1446776877081-d282a0f896e2?w=800&h=600&fit=crop";
    }
}