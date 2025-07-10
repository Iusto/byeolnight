package com.byeolnight.service.video;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class YouTubeService {
    
    private RestTemplate restTemplate;
    
    @Value("${google.api.key:}")
    private String googleApiKey;
    
    public YouTubeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        log.info("YouTubeService 초기화 완료");
    }
    
    private static final String YOUTUBE_API_URL = "https://www.googleapis.com/youtube/v3/search";
    
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchSpaceVideos() {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(YOUTUBE_API_URL)
                    .queryParam("key", googleApiKey)
                    .queryParam("part", "snippet")
                    .queryParam("q", "우주 천문학 NASA 스페이스X 태양계")
                    .queryParam("type", "video")
                    .queryParam("maxResults", "12")
                    .queryParam("order", "relevance")
                    .queryParam("regionCode", "KR")
                    .queryParam("relevanceLanguage", "ko")
                    .build()
                    .toUriString();
            
            log.info("YouTube API 호출: 우주 관련 영상 검색");
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                var items = (List<Map<String, Object>>) response.getBody().get("items");
                log.info("YouTube 영상 검색 성공: {}개", items != null ? items.size() : 0);
                return items;
            }
            
            log.warn("YouTube API 호출 실패");
            return List.of();
            
        } catch (Exception e) {
            log.error("YouTube 영상 검색 중 오류 발생", e);
            return List.of();
        }
    }
    
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchVideosByKeyword(String keyword) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(YOUTUBE_API_URL)
                    .queryParam("key", googleApiKey)
                    .queryParam("part", "snippet")
                    .queryParam("q", keyword + " 우주")
                    .queryParam("type", "video")
                    .queryParam("maxResults", "8")
                    .queryParam("order", "relevance")
                    .queryParam("regionCode", "KR")
                    .queryParam("relevanceLanguage", "ko")
                    .build()
                    .toUriString();
            
            log.info("YouTube API 호출: {} 관련 영상 검색", keyword);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                var items = (List<Map<String, Object>>) response.getBody().get("items");
                return items != null ? items : List.of();
            }
            
            return List.of();
            
        } catch (Exception e) {
            log.error("YouTube 키워드 검색 중 오류 발생: {}", keyword, e);
            return List.of();
        }
    }
}