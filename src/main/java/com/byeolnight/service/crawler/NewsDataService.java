package com.byeolnight.service.crawler;

import com.byeolnight.dto.ai.NewsApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsDataService {
    
    private final RestTemplate restTemplate;
    
    @Value("${newsdata.api.key}")
    private String apiKey;
    
    private static final String NEWS_API_URL = "https://newsdata.io/api/1/news";
    
    public NewsApiResponseDto fetchKoreanSpaceNews() {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(NEWS_API_URL)
                    .queryParam("apikey", apiKey)
                    .queryParam("language", "ko")  // 한국어만
                    .queryParam("q", "우주 OR 천문학 OR NASA OR 스페이스X OR 화성 OR 달 OR 위성 OR 항공우주")
                    .queryParam("size", "10")  // 최대 10개
                    .build()
                    .toUriString();
            
            log.info("NewsData.io API 호출: {}", url);
            
            NewsApiResponseDto response = restTemplate.getForObject(url, NewsApiResponseDto.class);
            
            if (response != null && "success".equals(response.getStatus())) {
                log.info("뉴스 수집 성공: {}개", response.getResults().size());
                return response;
            } else {
                log.warn("뉴스 수집 실패: {}", response != null ? response.getStatus() : "null response");
                return null;
            }
            
        } catch (Exception e) {
            log.error("NewsData.io API 호출 중 오류 발생", e);
            return null;
        }
    }
}