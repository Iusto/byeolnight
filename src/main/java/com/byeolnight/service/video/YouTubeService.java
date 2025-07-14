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
@RequiredArgsConstructor
public class YouTubeService {
    
    private final RestTemplate restTemplate;
    private final com.byeolnight.service.crawler.NewsDataService newsDataService;
    
    @Value("${google.api.key:}")
    private String googleApiKey;
    
    @Value("${youtube.search.max-results:12}")
    private int maxResults;
    
    @Value("${youtube.search.keyword-count:3}")
    private int keywordCount;
    
    private static final String YOUTUBE_API_URL = "https://www.googleapis.com/youtube/v3/search";
    
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchSpaceVideos() {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(YOUTUBE_API_URL)
                    .queryParam("key", googleApiKey)
                    .queryParam("part", "snippet")
                    .queryParam("q", getRandomSpaceQuery())
                    .queryParam("type", "video")
                    .queryParam("maxResults", String.valueOf(maxResults))
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
                    .queryParam("maxResults", String.valueOf(maxResults / 2))
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
    
    /**
     * 200개 우주 키워드에서 랜덤하게 선택하여 YouTube 검색 쿼리 생성
     */
    private String getRandomSpaceQuery() {
        java.util.Random random = new java.util.Random();
        java.util.Set<String> selectedKeywords = new java.util.HashSet<>();
        
        // 한국어 키워드에서 선택
        String[] koreanKeywords = newsDataService.getKoreanSpaceKeywords();
        while (selectedKeywords.size() < keywordCount && selectedKeywords.size() < koreanKeywords.length) {
            int randomIndex = random.nextInt(koreanKeywords.length);
            selectedKeywords.add(koreanKeywords[randomIndex]);
        }
        
        String query = String.join(" ", selectedKeywords);
        log.info("YouTube 검색 키워드: {}", query);
        return query;
    }
    
    /**
     * 중복 영상 체크 (videoId 기준)
     */
    private boolean isDuplicateVideo(List<Map<String, Object>> existingVideos, String videoId) {
        return existingVideos.stream()
                .anyMatch(video -> {
                    Map<String, Object> id = (Map<String, Object>) video.get("id");
                    return id != null && videoId.equals(id.get("videoId"));
                });
    }
    
    /**
     * 중복 제거된 영상 목록 반환
     */
    public List<Map<String, Object>> getUniqueSpaceVideos() {
        List<Map<String, Object>> allVideos = new java.util.ArrayList<>();
        java.util.Set<String> videoIds = new java.util.HashSet<>();
        
        // 여러 번 검색하여 다양한 영상 수집
        for (int i = 0; i < 3; i++) {
            List<Map<String, Object>> videos = searchSpaceVideos();
            for (Map<String, Object> video : videos) {
                Map<String, Object> id = (Map<String, Object>) video.get("id");
                if (id != null) {
                    String videoId = (String) id.get("videoId");
                    if (videoId != null && !videoIds.contains(videoId)) {
                        videoIds.add(videoId);
                        allVideos.add(video);
                    }
                }
            }
        }
        
        log.info("중복 제거 후 YouTube 영상: {}개", allVideos.size());
        return allVideos;
    }
}