package com.byeolnight.service.crawler;

import com.byeolnight.dto.ai.NewsDataResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class NewsApiClient {
    
    @Value("${newsdata.api.key}")
    private String apiKey;
    
    @Value("${newsdata.api.url:https://newsdata.io/api/1/news}")
    private String apiUrl;
    
    private final RestTemplate restTemplate;
    
    public NewsApiClient() {
        this.restTemplate = new RestTemplate();
    }
    
    public NewsDataResponseDto fetchSpaceNews() {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("apikey", apiKey)
                    .queryParam("q", "우주 OR NASA OR 천문학 OR 항공우주 OR 우주탐사")
                    .queryParam("language", "ko")
                    .queryParam("size", "10")
                    .toUriString();
            
            log.info("뉴스 API 호출: {}", url.replaceAll("apikey=[^&]*", "apikey=***"));
            
            NewsDataResponseDto response = restTemplate.getForObject(url, NewsDataResponseDto.class);
            
            if (response != null && "success".equals(response.getStatus())) {
                log.info("뉴스 API 호출 성공: {} 건의 뉴스 수신", response.getResults().size());
                return response;
            } else {
                log.warn("뉴스 API 응답 상태 이상: {}", response != null ? response.getStatus() : "null");
                return null;
            }
            
        } catch (Exception e) {
            log.error("뉴스 API 호출 실패", e);
            return null;
        }
    }
}