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
            // 먼저 한국어 뉴스 시도
            log.info("한국어 우주 뉴스를 수집합니다.");
            NewsApiResponseDto koreanNews = fetchNewsByLanguage("ko", "\"우주 탐사\" OR \"우주 발사\" OR \"우주선\" OR \"로켓 발사\" OR \"위성 발사\" OR \"화성 탐사\" OR \"달 탐사\" OR \"우주정거장\" OR \"우주비행사\" OR \"천문학 발견\" OR \"블랙홀\" OR \"은하\" OR \"혜성\" OR \"소행성\" OR \"망원경\"");
            
            // 한국어 뉴스가 충분하지 않으면 영어 뉴스도 수집
            if (koreanNews == null || koreanNews.getResults().size() < 3) {
                log.info("한국어 뉴스가 부족하여 영어 뉴스도 수집합니다.");
                NewsApiResponseDto englishNews = fetchNewsByLanguage("en", "\"NASA launch\" OR \"SpaceX launch\" OR \"Mars mission\" OR \"space exploration\" OR \"rocket launch\" OR \"satellite launch\" OR \"astronaut mission\" OR \"space discovery\" OR \"astronomy discovery\" OR \"black hole\" OR \"galaxy discovery\" OR \"telescope discovery\" OR \"space station\" OR \"spacecraft\" OR \"asteroid\" OR \"comet\"");
                
                if (englishNews != null && englishNews.getResults().size() > 0) {
                    if (koreanNews == null) {
                        return englishNews;
                    } else {
                        // 한국어와 영어 뉴스 합치기
                        koreanNews.getResults().addAll(englishNews.getResults());
                        koreanNews.setTotalResults(koreanNews.getTotalResults() + englishNews.getTotalResults());
                    }
                }
            }
            
            return koreanNews;
            
        } catch (Exception e) {
            log.error("NewsData.io API 호출 중 오류 발생", e);
            return null;
        }
    }
    
    private NewsApiResponseDto fetchNewsByLanguage(String language, String query) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(NEWS_API_URL)
                    .queryParam("apikey", apiKey)
                    .queryParam("language", language)
                    .queryParam("q", query)
                    .queryParam("size", "2")
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
            log.error("{} NewsData.io API 호출 중 오류 발생", language, e);
            return null;
        }
    }
}