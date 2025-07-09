package com.byeolnight.service.crawler;

import com.byeolnight.domain.entity.News;
import com.byeolnight.domain.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsTestService {
    
    private final NewsRepository newsRepository;
    
    @EventListener(ApplicationReadyEvent.class)
    public void initTestNews() {
        if (newsRepository.count() == 0) {
            log.info("테스트용 우주 뉴스 데이터 생성");
            
            News testNews = News.builder()
                    .title("NASA, 새로운 우주 탐사 계획 발표")
                    .description("NASA가 2025년 화성 탐사를 위한 새로운 계획을 발표했습니다. 이번 계획에는 인간의 화성 착륙을 위한 준비 단계가 포함되어 있습니다.")
                    .imageUrl("https://images.unsplash.com/photo-1446776877081-d282a0f896e2?w=800&h=600&fit=crop")
                    .url("https://example.com/nasa-mars-mission")
                    .publishedAt(LocalDateTime.now())
                    .hashtags("#우주 #NASA #화성 #탐사")
                    .source("NASA")
                    .summary("")
                    .build();
            
            newsRepository.save(testNews);
            log.info("테스트 뉴스 저장 완료: {}", testNews.getTitle());
        }
    }
}