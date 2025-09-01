package com.byeolnight.service.crawler;

import com.byeolnight.entity.News;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.post.Post.Category;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.NewsRepository;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.dto.ai.NewsApiResponseDto;
import com.byeolnight.infrastructure.config.NewsCollectionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpaceNewsService {
    
    private final NewsRepository newsRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final NewsCollectionProperties newsConfig;
    private final NewsContentValidator validator;
    private final NewsTranslationService translationService;
    private final NewsContentFormatter formatter;
    
    @Value("${app.security.external-api.ai.newsdata-api-key}")
    private String primaryApiKey;
    
    @Value("${app.newsdata.api-key-backup:}")
    private String backupApiKey;
    
    private boolean usingBackupKey = false;
    private static final String NEWS_API_URL = "https://newsdata.io/api/1/news";
    private static final String[] KOREAN_KEYWORDS = {"우주", "로켓", "위성", "화성", "달", "NASA", "SpaceX", "우주탐사", "화성탐사", "달탐사", "태양", "지구", "목성", "토성", "블랙홀", "은하", "별", "항성", "혜성", "소행성", "망원경", "천문", "항공우주", "우주선", "우주정거장", "우주비행사"};
    private static final String[] ENGLISH_KEYWORDS = {"NASA", "SpaceX", "Mars", "Moon", "space exploration", "astronomy", "telescope", "satellite", "rocket", "space", "planet", "solar", "lunar", "jupiter", "saturn", "galaxy", "nebula", "star", "comet", "asteroid", "orbit", "spacecraft", "astronaut", "eclipse", "aurora", "supernova", "exoplanet", "hubble", "webb", "iss", "falcon", "dragon", "starship", "artemis", "apollo", "voyager", "perseverance", "curiosity"};
    
    @Transactional
    public void collectAndSaveSpaceNews() {
        log.info("한국어 우주 뉴스 수집 시작");
        
        NewsApiResponseDto response = fetchKoreanSpaceNews();
        if (response == null || response.getResults() == null) {
            log.warn("뉴스 데이터를 가져올 수 없습니다");
            return;
        }
        
        // 뉴스봇 사용자 가져오기
        User newsBot = userRepository.findByEmail("newsbot@byeolnight.com")
                .orElseThrow(() -> new RuntimeException("뉴스봇 사용자를 찾을 수 없습니다"));
        
        List<Post> savedPosts = new ArrayList<>();
        int actualDuplicateCount = 0;
        int filteredCount = 0;
        
        for (NewsApiResponseDto.Result result : response.getResults()) {
            log.info("\n========== 뉴스 처리 시작 ==========\n제목: {}\nURL: {}", result.getTitle(), result.getLink());
            
            if (isDuplicateNews(result)) {
                actualDuplicateCount++;
                log.info("중복으로 스킵됨");
                continue;
            }
            
            if (!validator.isHighQualityNews(result)) {
                filteredCount++;
                log.info("품질 기준 미달로 스킵됨");
                continue;
            }
            
            // 설정된 최대 개수만 저장 (하루에 1개만 저장)
            if (savedPosts.size() >= newsConfig.getCollection().getMaxPosts()) {
                log.info("이미 {}개 뉴스를 저장했으므로 종료 (하루 1개 제한)", newsConfig.getCollection().getMaxPosts());
                break;
            }
            
            log.info("저장 진행 중... ({}/1)", savedPosts.size() + 1);
            
            // News 엔티티에 저장
            News news = convertToNews(result);
            newsRepository.save(news);
            
            // Post 엔티티로 변환하여 게시판에 표시
            Post post = convertToPost(result, newsBot);
            Post savedPost = postRepository.save(post);
            savedPosts.add(savedPost);
            
            log.info("새 뉴스 게시글 저장: {}", savedPost.getTitle());
        }
        
        log.info("우주 뉴스 수집 완료 - 수집: {}개, 저장: {}건 (하루 최대 {}), 실제 중복: {}건, 필터링: {}건", 
                response.getResults().size(), savedPosts.size(), newsConfig.getCollection().getMaxPosts(), actualDuplicateCount, filteredCount);
        
        // 뉴스 수집과 토론 주제 생성을 분리
        // 토론 주제는 별도 스케줄러에서 매일 오전 8시에 생성
        log.info("뉴스 수집 완료 - 토론 주제는 스케줄러에서 별도 처리");
    }
    
    private boolean isDuplicateNews(NewsApiResponseDto.Result result) {
        return newsRepository.existsByUrl(result.getLink());
    }
    

    

    

    

    

    
    private Post convertToPost(NewsApiResponseDto.Result result, User writer) {
        String content = formatter.formatNewsContent(result);
        String title = translationService.translateTitle(result.getTitle());
        
        if (title.length() > 100) {
            title = title.substring(0, 97) + "...";
        }
        
        return Post.builder()
                .title(title)
                .content(content)
                .category(Category.NEWS)
                .writer(writer)
                .build();
    }
    

    

    
    private News convertToNews(NewsApiResponseDto.Result result) {
        String title = translationService.translateTitle(result.getTitle());
        
        return News.builder()
                .title(title)
                .description(result.getDescription())
                .imageUrl(result.getImageUrl() != null ? result.getImageUrl() : getDefaultSpaceImage())
                .url(result.getLink())
                .publishedAt(parsePublishedAt(result.getPubDate()))
                .hashtags(formatter.generateHashtags(result.getTitle(), result.getDescription()))
                .source(result.getSourceName() != null ? result.getSourceName() : "Unknown")
                .summary(generateSummary(result))
                .build();
    }
    
    private LocalDateTime parsePublishedAt(String publishedAt) {
        try {
            // NewsData.io 날짜 형식: "2024-01-15 12:30:45"
            return LocalDateTime.parse(publishedAt, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            try {
                // ISO 형식도 시도
                return LocalDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception ex) {
                log.warn("발행일 파싱 실패: {}, 현재 시간으로 설정", publishedAt);
                return LocalDateTime.now();
            }
        }
    }
    

    
    private String getDefaultSpaceImage() {
        return "https://images.unsplash.com/photo-1446776877081-d282a0f896e2?w=800&h=600&fit=crop";
    }
    

    
    private String generateSummary(NewsApiResponseDto.Result result) {
        String summary = translationService.generateAIAnalysis(result.getTitle(), result.getDescription());
        return summary.length() > 100 ? summary.substring(0, 97) + "..." : summary;
    }
    
    // NewsDataService에서 이동된 메서드들
    public NewsApiResponseDto fetchKoreanSpaceNews() {
        try {
            int callCount = 4;
            
            String koreanQuery = "NASA OR SpaceX OR 우주탐사 OR 화성탐사 OR 달탐사";
            log.info("한국어 뉴스 수집 키워드: {}", koreanQuery);
            NewsApiResponseDto koreanNews = fetchMultipleNewsByLanguage("ko", koreanQuery, callCount / 2);
            
            String englishQuery = "NASA OR SpaceX OR Mars OR Moon OR space exploration OR astronomy";
            log.info("영어 뉴스 수집 키워드: {}", englishQuery);
            NewsApiResponseDto englishNews = fetchMultipleNewsByLanguage("en", englishQuery, callCount / 2);
            
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
            log.info("총 {}+{} = {}개 뉴스 수집 완료", 
                    koreanNews != null ? koreanNews.getResults().size() : 0,
                    englishNews != null ? englishNews.getResults().size() : 0,
                    combinedNews.getResults().size());
            
            return combinedNews;
            
        } catch (Exception e) {
            log.error("NewsData.io API 호출 중 오류 발생", e);
            return null;
        }
    }
    
    private NewsApiResponseDto fetchNewsByLanguage(String language, String query, int size) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(NEWS_API_URL)
                    .queryParam("apikey", getCurrentApiKey())
                    .queryParam("language", language)
                    .queryParam("q", query)
                    .queryParam("category", "science")
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
            
            if (isQuotaExceededError(e) && !usingBackupKey && !backupApiKey.isEmpty()) {
                log.warn("기본 API 키 한도 초과, 백업 키로 재시도합니다.");
                usingBackupKey = true;
                return fetchNewsByLanguage(language, query, size);
            }
            
            return null;
        }
    }
    
    private NewsApiResponseDto fetchMultipleNewsByLanguage(String language, String query, int callCount) {
        NewsApiResponseDto combinedResponse = new NewsApiResponseDto();
        combinedResponse.setStatus("success");
        combinedResponse.setResults(new java.util.ArrayList<>());
        java.util.Set<String> seenUrls = new java.util.HashSet<>();
        
        for (int i = 0; i < callCount; i++) {
            try {
                String[] keywords = language.equals("ko") ? KOREAN_KEYWORDS : ENGLISH_KEYWORDS;
                String randomQuery = getRandomSpaceKeywords(keywords, 3);
                
                NewsApiResponseDto response = fetchNewsByLanguage(language, randomQuery, 10);
                if (response != null && response.getResults() != null) {
                    for (NewsApiResponseDto.Result result : response.getResults()) {
                        if (result.getLink() != null && !seenUrls.contains(result.getLink())) {
                            seenUrls.add(result.getLink());
                            combinedResponse.getResults().add(result);
                        }
                    }
                }
                
                if (i < callCount - 1) {
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                log.warn("{} 뉴스 {}번째 호출 실패", language, i + 1, e);
            }
        }
        
        combinedResponse.setTotalResults(combinedResponse.getResults().size());
        log.info("{} 뉴스 {}번 호출 완료: {}개 수집", language, callCount, combinedResponse.getResults().size());
        return combinedResponse;
    }
    
    private String getRandomSpaceKeywords(String[] keywords, int count) {
        java.util.Random random = new java.util.Random();
        java.util.Set<String> selectedKeywords = new java.util.HashSet<>();
        
        // 필수 키워드 추가 (NASA, SpaceX, 우주 등)
        if (keywords == KOREAN_KEYWORDS) {
            selectedKeywords.add("NASA");
            selectedKeywords.add("SpaceX");
            selectedKeywords.add("우주");
        } else {
            selectedKeywords.add("NASA");
            selectedKeywords.add("SpaceX");
            selectedKeywords.add("space");
        }
        
        while (selectedKeywords.size() < count && selectedKeywords.size() < keywords.length) {
            int randomIndex = random.nextInt(keywords.length);
            selectedKeywords.add(keywords[randomIndex]);
        }
        
        return String.join(" OR ", selectedKeywords);
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
               errorMessage.contains("429");
    }
    
    public long getTodayNewsCount() {
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        
        // 오늘 작성된 NEWS 게시글 개수 조회
        return newsRepository.countByCreatedAtAfter(todayStart);
    }
}