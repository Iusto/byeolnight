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
    
    // 200개 우주 키워드 캐시드 배열
    private static final String[] ALL_SPACE_KEYWORDS_CACHED;
    
    // 100개 우주 키워드 (한국어)
    private static final String[] KOREAN_SPACE_KEYWORDS = {
        "우주", "로켓", "위성", "화성", "달", "태양", "지구", "목성", "토성", "천왕성",
        "해왕성", "수성", "금성", "명왕성", "블랙홀", "은하", "별", "항성", "혜성", "소행성",
        "망원경", "천문", "항공우주", "우주선", "우주정거장", "우주비행사", "우주발사", "우주탐사", "성운", "퀘이사",
        "중성자별", "백색왜성", "적색거성", "초신성", "성단", "성간물질", "암흑물질", "암흑에너지", "빅뱅", "우주론",
        "외계행성", "외계생명", "SETI", "우주망원경", "허블", "제임스웹", "케플러", "스피처", "찬드라", "컴프턴",
        "국제우주정거장", "ISS", "아르테미스", "아폴로", "보이저", "카시니", "갈릴레오", "뉴호라이즌스", "파커", "주노",
        "화성탐사", "달탐사", "목성탐사", "토성탐사", "태양탐사", "소행성탐사", "혜성탐사", "우주쓰레기", "우주날씨", "태양풍",
        "자기권", "오로라", "일식", "월식", "유성우", "운석", "크레이터", "화산", "대기", "중력",
        "궤도", "공전", "자전", "조석", "라그랑주점", "중력파", "상대성이론", "양자역학", "끈이론", "다중우주",
        "우주배경복사", "적색편이", "도플러효과", "허블상수", "우주나이", "우주크기", "관측가능우주", "사건지평선", "특이점", "웜홀"
    };
    
    // 100개 우주 키워드 (영어)
    private static final String[] ENGLISH_SPACE_KEYWORDS = {
        "space", "rocket", "satellite", "Mars", "Moon", "Sun", "Earth", "Jupiter", "Saturn", "Uranus",
        "Neptune", "Mercury", "Venus", "Pluto", "blackhole", "galaxy", "star", "stellar", "comet", "asteroid",
        "telescope", "astronomy", "aerospace", "spacecraft", "space station", "astronaut", "space launch", "space exploration", "nebula", "quasar",
        "neutron star", "white dwarf", "red giant", "supernova", "cluster", "interstellar", "dark matter", "dark energy", "big bang", "cosmology",
        "exoplanet", "extraterrestrial", "SETI", "space telescope", "Hubble", "James Webb", "Kepler", "Spitzer", "Chandra", "Compton",
        "ISS", "International Space Station", "Artemis", "Apollo", "Voyager", "Cassini", "Galileo", "New Horizons", "Parker", "Juno",
        "Mars exploration", "lunar exploration", "Jupiter mission", "Saturn mission", "solar mission", "asteroid mission", "comet mission", "space debris", "space weather", "solar wind",
        "magnetosphere", "aurora", "eclipse", "lunar eclipse", "meteor shower", "meteorite", "crater", "volcano", "atmosphere", "gravity",
        "orbit", "revolution", "rotation", "tidal", "Lagrange point", "gravitational wave", "relativity", "quantum mechanics", "string theory", "multiverse",
        "cosmic background", "redshift", "Doppler effect", "Hubble constant", "universe age", "universe size", "observable universe", "event horizon", "singularity", "wormhole"
    };
    
    // static 블록으로 키워드 배열 미리 캐싱
    static {
        ALL_SPACE_KEYWORDS_CACHED = java.util.stream.Stream.concat(
            java.util.Arrays.stream(KOREAN_SPACE_KEYWORDS),
            java.util.Arrays.stream(ENGLISH_SPACE_KEYWORDS)
        ).map(String::toLowerCase).toArray(String[]::new);
    }
    
    public NewsApiResponseDto fetchKoreanSpaceNews() {
        try {
            // 더 많은 뉴스를 가져와서 중복 제외 후 1개 선택
            // 최고 품질 뉴스를 위해 40개 수집 (10개 x 4번)
            int callCount = 4; // 총 40개 수집
            
            // 한국어 뉴스 수집 (더 구체적인 키워드 사용)
            String koreanQuery = "NASA OR SpaceX OR 우주탐사 OR 화성탐사 OR 달탐사";
            log.info("한국어 뉴스 수집 키워드: {}", koreanQuery);
            NewsApiResponseDto koreanNews = fetchMultipleNewsByLanguage("ko", koreanQuery, callCount / 2);
            
            // 영어 뉴스 수집 (더 구체적인 키워드 사용)
            String englishQuery = "NASA OR SpaceX OR Mars OR Moon OR space exploration OR astronomy";
            log.info("영어 뉴스 수집 키워드: {}", englishQuery);
            NewsApiResponseDto englishNews = fetchMultipleNewsByLanguage("en", englishQuery, callCount / 2);
            
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
            log.info("총 {}+{} = {}개 뉴스 수집 완료 (중복 제외 전, 하루 400개 한도 내, 10개 x {}번)",
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
                    .queryParam("category", "science") // 과학 카테고리 추가
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
     * 다중 호출로 더 많은 뉴스 수집
     */
    private NewsApiResponseDto fetchMultipleNewsByLanguage(String language, String query, int callCount) {
        NewsApiResponseDto combinedResponse = new NewsApiResponseDto();
        combinedResponse.setStatus("success");
        combinedResponse.setResults(new java.util.ArrayList<>());
        java.util.Set<String> seenUrls = new java.util.HashSet<>();
        
        for (int i = 0; i < callCount; i++) {
            try {
                // 매번 다른 키워드로 호출하여 다양성 확보
                String[] keywords = language.equals("ko") ? KOREAN_SPACE_KEYWORDS : ENGLISH_SPACE_KEYWORDS;
                String randomQuery = getRandomSpaceKeywords(keywords, 3);
                
                NewsApiResponseDto response = fetchNewsByLanguage(language, randomQuery, 10);
                if (response != null && response.getResults() != null) {
                    // 중복 URL 제거
                    for (NewsApiResponseDto.Result result : response.getResults()) {
                        if (result.getLink() != null && !seenUrls.contains(result.getLink())) {
                            seenUrls.add(result.getLink());
                            combinedResponse.getResults().add(result);
                        }
                    }
                }
                
                // API 요청 간격 (Rate Limit 방지)
                if (i < callCount - 1) {
                    Thread.sleep(200); // 0.2초 대기
                }
            } catch (Exception e) {
                log.warn("{} 뉴스 {}번째 호출 실패", language, i + 1, e);
            }
        }
        
        combinedResponse.setTotalResults(combinedResponse.getResults().size());
        log.info("{} 뉴스 {}번 호출 완료: {}개 수집 (중복 제거됨)", language, callCount, combinedResponse.getResults().size());
        return combinedResponse;
    }
    
    /**
     * 랜덤하게 우주 키워드 선택하여 쿼리 생성
     */
    private String getRandomSpaceKeywords(String[] keywords, int count) {
        java.util.Random random = new java.util.Random();
        java.util.Set<String> selectedKeywords = new java.util.HashSet<>();
        
        while (selectedKeywords.size() < count && selectedKeywords.size() < keywords.length) {
            int randomIndex = random.nextInt(keywords.length);
            selectedKeywords.add(keywords[randomIndex]);
        }
        
        return String.join(" OR ", selectedKeywords);
    }
    
    public String[] getKoreanSpaceKeywords() {
        return KOREAN_SPACE_KEYWORDS;
    }
    
    public String[] getEnglishSpaceKeywords() {
        return ENGLISH_SPACE_KEYWORDS;
    }
    
    public String[] getAllSpaceKeywordsCached() {
        return ALL_SPACE_KEYWORDS_CACHED;
    }
}