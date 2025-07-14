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
            // 적응형 수집량: 초기 40개 -> 장기적으로 80개까지 증가
            int fetchSize = calculateOptimalFetchSize();
            
            log.info("한국어 우주 뉴스를 수집합니다 (적응형 수집량: {}개).", fetchSize);
            NewsApiResponseDto koreanNews = fetchNewsByLanguage("ko", "우주 OR 로켓 OR 위성 OR 화성 OR NASA OR SpaceX", fetchSize);
            
            log.info("영어 우주 뉴스도 수집합니다 (적응형 수집량: {}개).", fetchSize);
            NewsApiResponseDto englishNews = fetchNewsByLanguage("en", "NASA OR SpaceX OR space OR rocket OR Mars OR satellite", fetchSize);
            
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
            log.info("총 {}+{} = {}개 뉴스 수집 완료 (중복 제외 전, 하루 400개 한도 내, 수집량: {}x2)", 
                    koreanNews != null ? koreanNews.getResults().size() : 0,
                    englishNews != null ? englishNews.getResults().size() : 0,
                    combinedNews.getResults().size(), fetchSize);
            
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
     * DB 뉴스 개수 기반 적응형 수집량 계산
     * - 0~100개: 20개씩 (총 40개)
     * - 100~500개: 30개씩 (총 60개)
     * - 500~1000개: 40개씩 (총 80개)
     * - 1000개+: 50개씩 (총 100개)
     */
    private int calculateOptimalFetchSize() {
        try {
            // NewsRepository를 직접 주입받지 않고 서비스를 통해 가져오기
            // 임시로 기본값 사용 (나중에 수정 예정)
            long newsCount = newsRepository.count(); // DB에 저장된 뉴스 개수
            
            int fetchSize;
            if (newsCount < 100) {
                fetchSize = 20; // 초기 단계
            } else if (newsCount < 500) {
                fetchSize = 30; // 중간 단계
            } else if (newsCount < 1000) {
                fetchSize = 40; // 고급 단계
            } else {
                fetchSize = 50; // 최대 단계
            }
            
            log.info("수집량 계산: DB 뉴스 {}개 -> 언어별 {}개 (총 {}x2개)", newsCount, fetchSize, fetchSize);
            return fetchSize;
            
        } catch (Exception e) {
            log.warn("수집량 계산 실패, 기본값 20 사용", e);
            return 20;
        }
    }
}