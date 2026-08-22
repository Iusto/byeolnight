package com.byeolnight.service.crawler;

import com.byeolnight.dto.ai.NewsApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** NewsData.io 호출, 키 전환, 응답 병합만 담당하는 외부 API 어댑터다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsDataClient {

    private static final String NEWS_API_URL = "https://newsdata.io/api/1/news";
    private static final String[] KOREAN_TOPIC_QUERIES = {
            "NASA OR 달 탐사 OR 아르테미스", "SpaceX OR 로켓 발사 OR 우주선",
            "천문학 OR 외계행성 OR 우주망원경", "소행성 OR 태양 폭풍 OR 우주날씨",
            "화성 탐사 OR 우주정거장 OR 인공위성"
    };
    private static final String[] ENGLISH_TOPIC_QUERIES = {
            "NASA OR Artemis OR lunar mission", "SpaceX OR Starship OR rocket launch",
            "astronomy OR exoplanet OR space telescope", "asteroid OR solar flare OR space weather",
            "Mars mission OR space station OR satellite"
    };

    private final RestTemplate restTemplate;

    @Value("${app.security.external-api.ai.newsdata-api-key}")
    private String primaryApiKey;

    @Value("${app.security.external-api.ai.newsdata-api-key-backup:}")
    private String backupApiKey;

    private boolean usingBackupKey;

    /** 한국어·영어 기사를 각각 두 번 조회하고 URL 기준으로 합친다. */
    public NewsApiResponseDto fetchSpaceNews() {
        try {
            NewsApiResponseDto koreanNews = fetchMultipleNewsByLanguage("ko", 2);
            NewsApiResponseDto englishNews = fetchMultipleNewsByLanguage("en", 2);

            NewsApiResponseDto combined = emptyResponse();
            if (koreanNews.getResults() != null) {
                combined.getResults().addAll(koreanNews.getResults());
            }
            if (englishNews.getResults() != null) {
                combined.getResults().addAll(englishNews.getResults());
            }
            combined.setTotalResults(combined.getResults().size());
            log.info("우주 뉴스 수집 완료: 한국어 {}건, 영어 {}건, 전체 {}건",
                    koreanNews.getResults().size(), englishNews.getResults().size(), combined.getResults().size());
            return combined;
        } catch (Exception exception) {
            Integer statusCode = getHttpStatusCode(exception);
            // API 키와 요청 URL은 로그에 남기지 않는다.
            log.error("NewsData.io 수집 실패: type={}, status={}",
                    exception.getClass().getSimpleName(), statusCode != null ? statusCode : "N/A");
            return null;
        }
    }

    private NewsApiResponseDto fetchMultipleNewsByLanguage(String language, int callCount) {
        NewsApiResponseDto combined = emptyResponse();
        Set<String> seenUrls = new HashSet<>();
        String[] queries = "ko".equals(language) ? KOREAN_TOPIC_QUERIES : ENGLISH_TOPIC_QUERIES;

        for (int index = 0; index < callCount; index++) {
            try {
                String query = queries[(LocalDate.now().getDayOfYear() + index) % queries.length];
                NewsApiResponseDto response = fetchNewsByLanguage(language, query, 10);
                if (response != null && response.getResults() != null) {
                    response.getResults().stream()
                            .filter(result -> result.getLink() != null && seenUrls.add(result.getLink()))
                            .forEach(combined.getResults()::add);
                }
                if (index < callCount - 1) {
                    Thread.sleep(200);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception exception) {
                log.warn("뉴스 반복 수집 실패: language={}, attempt={}, type={}",
                        language, index + 1, exception.getClass().getSimpleName());
            }
        }
        combined.setTotalResults(combined.getResults().size());
        return combined;
    }

    private NewsApiResponseDto fetchNewsByLanguage(String language, String query, int size) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(NEWS_API_URL)
                    .queryParam("apikey", currentApiKey())
                    .queryParam("language", language)
                    .queryParam("q", query)
                    .queryParam("category", "science")
                    .queryParam("size", size)
                    .build().toUriString();
            NewsApiResponseDto response = restTemplate.getForObject(url, NewsApiResponseDto.class);
            return response != null && "success".equals(response.getStatus()) ? response : null;
        } catch (Exception exception) {
            Integer statusCode = getHttpStatusCode(exception);
            log.error("NewsData.io API 호출 실패: language={}, type={}, status={}",
                    language, exception.getClass().getSimpleName(), statusCode != null ? statusCode : "N/A");
            if (statusCode != null && statusCode == 429 && !usingBackupKey && hasBackupKey()) {
                log.warn("NewsData.io 기본 키 한도 초과로 백업 키를 사용합니다.");
                usingBackupKey = true;
                return fetchNewsByLanguage(language, query, size);
            }
            return null;
        }
    }

    private NewsApiResponseDto emptyResponse() {
        NewsApiResponseDto response = new NewsApiResponseDto();
        response.setStatus("success");
        response.setResults(new ArrayList<>());
        return response;
    }

    private String currentApiKey() {
        return usingBackupKey ? backupApiKey : primaryApiKey;
    }

    private boolean hasBackupKey() {
        return backupApiKey != null && !backupApiKey.isBlank();
    }

    private Integer getHttpStatusCode(Exception exception) {
        return exception instanceof RestClientResponseException responseException
                ? responseException.getStatusCode().value() : null;
    }
}
