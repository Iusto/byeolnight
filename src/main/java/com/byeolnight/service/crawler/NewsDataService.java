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
    private final com.byeolnight.domain.repository.NewsRepository newsRepository;
    
    @Value("${newsdata.api.key}")
    private String primaryApiKey;
    
    @Value("${newsdata.api.key.backup:}")
    private String backupApiKey;
    
    private boolean usingBackupKey = false;
    
    private static final String NEWS_API_URL = "https://newsdata.io/api/1/news";
    
    public NewsApiResponseDto fetchKoreanSpaceNews() {
        try {
            // 더 많은 뉴스를 가져와서 중복 제외 후 2개 선택
            // API 제한으로 인해 10개씩 다중 호출로 더 많이 수집
            int callCount = calculateOptimalCallCount();
            
            log.info("한국어 우주 뉴스를 수집합니다 (10개 x {}번 호출).", callCount);
            NewsApiResponseDto koreanNews = fetchMultipleNewsByLanguage("ko", "우주 OR 로켓 OR 위성 OR 화성 OR NASA OR SpaceX", callCount);
            
            log.info("영어 우주 뉴스도 수집합니다 (10개 x {}번 호출).", callCount);
            NewsApiResponseDto englishNews = fetchMultipleNewsByLanguage("en", "NASA OR SpaceX OR space OR rocket OR Mars OR satellite", callCount);
            
            // 한국어와 영어 뉴스 합치기
            NewsApiResponseDto combinedNews = new NewsApiResponseDto();
            combinedNews.setStatus("success");
            combinedNews.setResults(new java.util.ArrayList<>());
            
            if (koreanNews != null && koreanNews.getResults() != null) {
                combinedNews.getResults().addAll(koreanNews.getResults());
            }
            if (englishNews != null && englishNews.getResults() != null) {
                combinedNews.getResults().addAll(englishNews.getResults());
            }
            
            combinedNews.setTotalResults(combinedNews.getResults().size());
            log.info("총 {}+{} = {}개 뉴스 수집 완룼 (중복 제외 전, 하루 400개 한도 내, 10개 x {}번)", 
                    koreanNews != null ? koreanNews.getResults().size() : 0,
                    englishNews != null ? englishNews.getResults().size() : 0,
                    combinedNews.getResults().size(), callCount);
            
            return combinedNews;
            
        } catch (Exception e) {
            log.error("NewsData.io API 호출 중 오류 발생", e);
            return null;
        }
    }
    
    private NewsApiResponseDto fetchNewsByLanguage(String language, String query) {
        return fetchNewsByLanguage(language, query, 1); // 기본 1개
    }
    
    private NewsApiResponseDto fetchNewsByLanguage(String language, String query, int size) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(NEWS_API_URL)
                    .queryParam("apikey", getCurrentApiKey())
                    .queryParam("language", language)
                    .queryParam("q", query)
                    .queryParam("size", String.valueOf(size))
                    .build()
                    .toUriString();
            
            log.info("NewsData.io API 호출 ({}): {}", language, url);
            
            NewsApiResponseDto response = restTemplate.getForObject(url, NewsApiResponseDto.class);
            
            if (response != null && "success".equals(response.getStatus())) {
                log.info("{} 뉴스 수집 성공: {}개", language, response.getResults().size());
                return response;
            } else {
                log.warn("{} 뉴스 수집 실패: {}", language, response != null ? response.getStatus() : "null response");
                return null;
            }
            
        } catch (Exception e) {
            log.error("{} NewsData.io API 호출 중 오류 발생: {}", language, e.getMessage());
            
            // API 한도 초과 오류인 경우 백업 키로 재시도
            if (isQuotaExceededError(e) && !usingBackupKey && !backupApiKey.isEmpty()) {
                log.warn("기본 API 키 한도 초과, 백업 키로 재시도합니다.");
                usingBackupKey = true;
                return fetchNewsByLanguage(language, query, size); // 백업 키로 재시도
            }
            
            return null;
        }
    }
    
    private String getCurrentApiKey() {
        String currentKey = usingBackupKey ? backupApiKey : primaryApiKey;
        log.debug("현재 사용 중인 API 키: {} (백업 키 사용: {})", 
                currentKey.substring(0, Math.min(10, currentKey.length())) + "...", usingBackupKey);
        return currentKey;
    }
    
    private boolean isQuotaExceededError(Exception e) {
        String errorMessage = e.getMessage().toLowerCase();
        return errorMessage.contains("quota") || 
               errorMessage.contains("limit") || 
               errorMessage.contains("exceeded") ||
               errorMessage.contains("429"); // HTTP 429 Too Many Requests
    }
    
    /**
     * DB 뉴스 개수 기반 적응형 호출 횟수 계산
     * - 0~100개: 2번 호출 (총 20개)
     * - 100~500개: 3번 호출 (총 30개)
     * - 500개+: 4번 호출 (총 40개)
     */
    private int calculateOptimalCallCount() {
        try {
            long newsCount = newsRepository.count();
            
            int callCount;
            if (newsCount < 100) {
                callCount = 2; // 10개 x 2번 = 20개
            } else if (newsCount < 500) {
                callCount = 3; // 10개 x 3번 = 30개
            } else {
                callCount = 4; // 10개 x 4번 = 40개
            }
            
            log.info("호출 횟수 계산: DB 뉴스 {}개 -> 언어별 {}번 호출 (총 {}x2개)", newsCount, callCount, callCount * 10);
            return callCount;
            
        } catch (Exception e) {
            log.warn("호출 횟수 계산 실패, 기본값 2 사용", e);
            return 2;
        }
    }
    
    /**
     * 다중 호출로 더 많은 뉴스 수집
     */
    private NewsApiResponseDto fetchMultipleNewsByLanguage(String language, String query, int callCount) {
        NewsApiResponseDto combinedResponse = new NewsApiResponseDto();
        combinedResponse.setStatus("success");
        combinedResponse.setResults(new java.util.ArrayList<>());
        
        for (int i = 0; i < callCount; i++) {
            try {
                NewsApiResponseDto response = fetchNewsByLanguage(language, query, 10);
                if (response != null && response.getResults() != null) {
                    combinedResponse.getResults().addAll(response.getResults());
                }
                
                // API 요청 간격 (Rate Limit 방지)
                if (i < callCount - 1) {
                    Thread.sleep(100); // 0.1초 대기
                }
            } catch (Exception e) {
                log.warn("{} 뉴스 {}번째 호출 실패", language, i + 1, e);
            }
        }
        
        combinedResponse.setTotalResults(combinedResponse.getResults().size());
        log.info("{} 뉴스 {}번 호출 완료: {}개 수집", language, callCount, combinedResponse.getResults().size());
        return combinedResponse;
    }
}