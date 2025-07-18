package com.byeolnight.service.cinema;

import com.byeolnight.domain.entity.Cinema;
import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.CinemaRepository;
import com.byeolnight.domain.repository.post.PostRepository;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.service.crawler.NewsDataService;
import com.byeolnight.infrastructure.config.CinemaCollectionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CinemaService {

    private final CinemaRepository cinemaRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NewsDataService newsDataService;
    private final CinemaCollectionProperties cinemaConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.api.key:}")
    private String googleApiKey;
    
    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Scheduled(cron = "0 0 20 * * ?") // 매일 오후 8시
    @Transactional
    public void createDailyCinemaPost() {
        try {
            log.info("별빛 시네마 자동 포스팅 시작");
            User systemUser = getSystemUser();
            collectAndSaveSpaceVideo(systemUser);
        } catch (Exception e) {
            log.error("별빛 시네마 자동 포스팅 실패", e);
        }
    }

    public void createCinemaPostManually(User admin) {
        try {
            log.info("수동 별빛 시네마 포스팅 시작 - 관리자: {}", admin.getNickname());
            collectAndSaveSpaceVideo(admin);
            log.info("수동 별빛 시네마 포스팅 성공");
        } catch (Exception e) {
            log.error("수동 별빛 시네마 포스팅 실패", e);
            throw new RuntimeException("별빛 시네마 포스팅에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void collectAndSaveSpaceVideo(User user) {
        log.info("우주 영상 수집 시작");
        
        Map<String, Object> videoData = fetchRandomSpaceVideo();
        if (videoData == null) {
            log.warn("영상 데이터를 가져올 수 없습니다");
            return;
        }
        
        String videoId = (String) videoData.get("videoId");
        String title = (String) videoData.get("title");
        
        // 중복 체크
        if (isDuplicateVideo(videoId, title)) {
            log.info("중복 영상으로 스킵: {}", title);
            return;
        }
        
        // Cinema 엔티티 저장
        Cinema cinema = convertToCinema(videoData);
        cinemaRepository.save(cinema);
        
        // Post 엔티티로 변환하여 게시판에 표시
        Post post = convertToPost(videoData, user);
        Post savedPost = postRepository.save(post);
        
        log.info("새 별빛시네마 게시글 저장: {}", savedPost.getTitle());
    }
    
    private boolean isDuplicateVideo(String videoId, String title) {
        boolean duplicateById = cinemaRepository.existsByVideoId(videoId);
        boolean duplicateByTitle = cinemaRepository.existsByTitle(title);
        
        log.info("=== 중복 체크 ===\n제목: {}\nVideo ID: {}\n중복 여부: {}", 
                title, videoId, duplicateById || duplicateByTitle);
        
        return duplicateById || duplicateByTitle;
    }
    
    private Cinema convertToCinema(Map<String, Object> videoData) {
        return Cinema.builder()
                .title((String) videoData.get("title"))
                .description((String) videoData.get("description"))
                .videoId((String) videoData.get("videoId"))
                .videoUrl((String) videoData.get("videoUrl"))
                .channelTitle((String) videoData.get("channelTitle"))
                .publishedAt((LocalDateTime) videoData.get("publishedAt"))
                .summary((String) videoData.get("summary"))
                .hashtags((String) videoData.get("hashtags"))
                .build();
    }
    
    private Post convertToPost(Map<String, Object> videoData, User writer) {
        return Post.builder()
                .title((String) videoData.get("title"))
                .content((String) videoData.get("content"))
                .category(Post.Category.STARLIGHT_CINEMA)
                .writer(writer)
                .build();
    }

    private Map<String, Object> fetchRandomSpaceVideo() {
        if (googleApiKey == null || googleApiKey.isEmpty()) {
            return createMockVideoData();
        }

        try {
            // 한국어 키워드로 먼저 시도
            List<String> koreanKeywords = Arrays.asList(newsDataService.getKoreanSpaceKeywords());
            Map<String, Object> koreanVideo = fetchVideoByLanguage(koreanKeywords, "ko");
            if (koreanVideo != null) {
                return koreanVideo;
            }
            
            // 영어 키워드로 시도
            List<String> englishKeywords = Arrays.asList(newsDataService.getEnglishSpaceKeywords());
            Map<String, Object> englishVideo = fetchVideoByLanguage(englishKeywords, "en");
            if (englishVideo != null) {
                return englishVideo;
            }
            
        } catch (Exception e) {
            log.error("YouTube API 호출 실패", e);
        }

        return createMockVideoData();
    }
    
    private Map<String, Object> fetchVideoByLanguage(List<String> keywords, String language) {
        for (int attempt = 0; attempt < cinemaConfig.getCollection().getRetryCount(); attempt++) {
            try {
                String query = getRandomKeywords(keywords, cinemaConfig.getCollection().getKeywordCount());
                
                String url = String.format(
                    "https://www.googleapis.com/youtube/v3/search?part=snippet&q=%s&type=video&maxResults=%d&order=relevance&publishedAfter=%s&videoDuration=%s&videoDefinition=%s&key=%s",
                    query, cinemaConfig.getQuality().getMaxResults(), getPublishedAfterDate(), 
                    cinemaConfig.getQuality().getVideoDuration(), cinemaConfig.getQuality().getVideoDefinition(), googleApiKey
                );
                
                log.info("YouTube API 호출 시도 {}/{}: {}", attempt + 1, cinemaConfig.getCollection().getRetryCount(), query);
            
                @SuppressWarnings("unchecked")
                Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                if (response != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                    
                    if (items != null && !items.isEmpty()) {
                        List<Map<String, Object>> qualityVideos = items.stream()
                            .filter(this::isQualityVideo)
                            .collect(java.util.stream.Collectors.toList());
                        
                        if (!qualityVideos.isEmpty()) {
                            Map<String, Object> video = qualityVideos.get(new Random().nextInt(qualityVideos.size()));
                            @SuppressWarnings("unchecked")
                            Map<String, Object> snippet = (Map<String, Object>) video.get("snippet");
                            @SuppressWarnings("unchecked")
                            Map<String, Object> videoId = (Map<String, Object>) video.get("id");
                            
                            String publishedAt = (String) snippet.get("publishedAt");
                            LocalDateTime publishDateTime = parsePublishedDateTime(publishedAt);
                            
                            Map<String, Object> videoData = formatVideoData(
                                (String) snippet.get("title"),
                                (String) snippet.get("description"),
                                (String) videoId.get("videoId"),
                                (String) snippet.get("channelTitle"),
                                publishDateTime
                            );
                            
                            // 유사도 체크
                            if (!isSimilarToExistingVideos(videoData)) {
                                return videoData;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("{} YouTube 영상 검색 시도 {}/{} 실패: {}", language, attempt + 1, cinemaConfig.getCollection().getRetryCount(), e.getMessage());
                if (attempt < cinemaConfig.getCollection().getRetryCount() - 1) {
                    try {
                        Thread.sleep(1000 * (attempt + 1)); // 점진적 대기
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        return null;
    }
    
    private boolean isQualityVideo(Map<String, Object> video) {
        @SuppressWarnings("unchecked")
        Map<String, Object> snippet = (Map<String, Object>) video.get("snippet");
        String title = (String) snippet.get("title");
        String channelTitle = (String) snippet.get("channelTitle");
        String description = (String) snippet.get("description");
        
        String titleLower = title.toLowerCase();
        String descLower = description != null ? description.toLowerCase() : "";
        String channelLower = channelTitle.toLowerCase();
        
        // 기본 품질 체크
        if (titleLower.contains("shorts") || titleLower.contains("#shorts") ||
            title.length() < cinemaConfig.getQuality().getMinTitleLength() ||
            (description != null && description.length() < cinemaConfig.getQuality().getMinDescriptionLength())) {
            return false;
        }
        
        // 생활용품/제품 관련 키워드 필터링 (2개 이상 발견 시 제외)
        int productKeywordCount = 0;
        for (String keyword : KEYWORDS.get("nonSpace")) {
            if (titleLower.contains(keyword)) {
                productKeywordCount++;
                if (productKeywordCount >= 2) {
                    return false;
                }
            }
        }
        
        // 음악/가사 관련 제외 (1개만 있어도 제외)
        for (String keyword : KEYWORDS.get("music")) {
            if (titleLower.contains(keyword)) {
                return false;
            }
        }
        
        // AI/기술 관련 키워드 제외 (우주 관련이 아닌 경우)
        for (String keyword : KEYWORDS.get("tech")) {
            if (titleLower.contains(keyword) && !hasSpaceContext(titleLower, descLower)) {
                return false;
            }
        }
        
        // 캐시된 우주 키워드 사용 (뉴스와 동일한 200개 키워드)
        String[] cachedKeywords = newsDataService.getAllSpaceKeywordsCached();
        
        boolean hasSpaceKeyword = false;
        boolean hasSpaceKeywordInTitle = false;
        
        for (String keyword : cachedKeywords) {
            if (titleLower.contains(keyword)) {
                hasSpaceKeywordInTitle = true;
                hasSpaceKeyword = true;
                break;
            } else if (descLower.contains(keyword)) {
                hasSpaceKeyword = true;
            }
        }
        
        // 제목에 우주 키워드가 없는 경우 더 엄격한 검증 필요
        if (!hasSpaceKeywordInTitle && hasSpaceKeyword) {
            // 제목에 우주 키워드가 없는 경우, 설명에 있는 키워드가 우주 맥락인지 추가 검증
            if (!hasStrongSpaceContext(titleLower, descLower)) {
                return false;
            }
        } else if (!hasSpaceKeyword) {
            return false;
        }
        
        // 고품질 채널 체크
        for (String channel : cinemaConfig.getYoutube().getQualityChannels()) {
            if (channelLower.contains(channel.toLowerCase())) {
                return true;
            }
        }
        
        // 전문 용어 체크
        for (String term : cinemaConfig.getYoutube().getProfessionalTerms()) {
            if (titleLower.contains(term) || descLower.contains(term)) {
                return true;
            }
        }
        
        return false;
    }
    
    // 모든 키워드를 하나의 정적 맵으로 통합
    private static final Map<String, String[]> KEYWORDS = Map.ofEntries(
        // 우주 관련 명확한 키워드
        Map.entry("space", new String[] {
            "우주", "space", "은하", "galaxy", "별자리", "행성", "planet",
            "태양계", "solar system", "nasa", "spacex", "블랙홀", "blackhole",
            "화성", "mars", "달탐사", "moon mission", "지구과학", "우주선", "spacecraft",
            "로켓발사", "rocket launch", "인공위성", "satellite", "천문학", "astronomy",
            "천문", "태양", "sun", "관측", "observation", "탐사", "exploration",
            "망원경", "telescope", "궤도", "orbit", "탐사선", "probe", "우주관측", "observatory"
        }),
        
        // 맥락이 필요한 키워드
        Map.entry("ambiguous", new String[] {
            "수성", "금성", "별", "달", "지구", "star", "moon", "earth"
        }),
        
        // 비우주 관련 키워드
        Map.entry("nonSpace", new String[] {
            "세제", "세정", "청소", "패널", "세척", "용액", "제품", "판매", "구매", "할인",
            "cleaner", "cleaning", "detergent", "wash", "panel", "product", "sale", "buy", "discount",
            "주방", "kitchen", "실내", "indoor", "실외", "outdoor", "생활", "lifestyle",
            "장바구니", "cart", "가격", "price", "상품", "item", "주문", "order",
            "코박고", "냉장고", "세탁기", "세탁", "냉장", "냉동", "사용법", "사용후기", "후기", "리뷰", 
            "review", "unboxing", "언박싱"
        }),
        
        // 음악 관련 키워드
        Map.entry("music", new String[] {
            "가사", "lyrics", "music video", "뮤직비디오", "노래", "song"
        }),
        
        // 기술 관련 키워드
        Map.entry("tech", new String[] {
            "ai", "인공지능", "특이점", "singularity", "머신러닝", "딥러닝", "chatgpt", "gpt", 
            "코딩", "프로그래밍", "coding", "programming"
        })
    );
    
    private boolean hasSpaceContext(String titleLower, String descLower) {
        // 명확한 우주 키워드 체크
        for (String keyword : KEYWORDS.get("space")) {
            if (titleLower.contains(keyword) || descLower.contains(keyword)) {
                return true;
            }
        }
        
        // 맥락이 필요한 키워드는 추가 검증 필요
        for (String keyword : KEYWORDS.get("ambiguous")) {
            if (titleLower.contains(keyword) || descLower.contains(keyword)) {
                // 해당 키워드가 있을 경우 우주 관련 맥락이 있는지 확인
                return hasAstronomyContext(titleLower, descLower);
            }
        }
        
        return false;
    }
    
    private boolean hasAstronomyContext(String titleLower, String descLower) {
        // 비우주 맥락이 있으면 우선 제외
        for (String nonContext : KEYWORDS.get("nonSpace")) {
            if (titleLower.contains(nonContext) || descLower.contains(nonContext)) {
                return false;
            }
        }
        
        // 우주 맥락이 있는지 확인
        for (String context : KEYWORDS.get("space")) {
            if (titleLower.contains(context) || descLower.contains(context)) {
                return true;
            }
        }
        
        // 기본적으로 맥락이 불분명하면 제외
        return false;
    }
    
    /**
     * 제목에 우주 키워드가 없는 경우 더 엄격한 검증을 위한 메서드
     */
    private boolean hasStrongSpaceContext(String titleLower, String descLower) {
        // 제목에 비우주 키워드가 있으면 바로 제외
        for (String nonKeyword : KEYWORDS.get("nonSpace")) {
            if (titleLower.contains(nonKeyword)) {
                return false;
            }
        }
        
        // 설명에 우주 핵심 키워드 개수 확인
        int spaceKeywordCount = 0;
        for (String keyword : KEYWORDS.get("space")) {
            if (descLower.contains(keyword)) {
                spaceKeywordCount++;
                if (spaceKeywordCount >= 2) { // 2개 이상 핵심 키워드가 있으면 우주 관련으로 판단
                    return true;
                }
            }
        }
        
        // 설명에 비우주 키워드가 있으면 제외
        for (String nonKeyword : KEYWORDS.get("nonSpace")) {
            if (descLower.contains(nonKeyword)) {
                return false;
            }
        }
        
        // 설명에 하나의 핵심 키워드가 있고 비우주 키워드가 없으면 허용
        return spaceKeywordCount > 0;
    }
    
    private String getPublishedAfterDate() {
        return LocalDateTime.now()
                .minusYears(cinemaConfig.getYoutube().getPublishedAfterYears())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
    }
    
    private String getRandomKeywords(List<String> keywords, int count) {
        Random random = new Random();
        Set<String> selectedKeywords = new HashSet<>();
        
        while (selectedKeywords.size() < count && selectedKeywords.size() < keywords.size()) {
            int randomIndex = random.nextInt(keywords.size());
            selectedKeywords.add(keywords.get(randomIndex));
        }
        
        return String.join(" OR ", selectedKeywords);
    }
    
    private boolean isSimilarToExistingVideos(Map<String, Object> videoData) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(cinemaConfig.getCollection().getSimilarityCheckDays());
        List<Cinema> recentVideos = cinemaRepository.findByCreatedAtAfter(cutoffDate);
        
        String newTitle = (String) videoData.get("title");
        
        for (Cinema cinema : recentVideos) {
            double similarity = calculateTitleSimilarity(newTitle, cinema.getTitle());
            if (similarity > cinemaConfig.getCollection().getSimilarityThreshold()) {
                log.info("유사 영상 발견 (유사도: {:.1f}%): {} vs {}", 
                        similarity * 100, newTitle, cinema.getTitle());
                return true;
            }
        }
        
        return false;
    }
    
    private double calculateTitleSimilarity(String title1, String title2) {
        String[] words1 = title1.toLowerCase().split("\\s+");
        String[] words2 = title2.toLowerCase().split("\\s+");
        
        int commonWords = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2) && word1.length() > 2) {
                    commonWords++;
                    break;
                }
            }
        }
        
        return (double) commonWords / Math.max(words1.length, words2.length);
    }

    private Map<String, Object> createMockVideoData() {
        String[] mockTitles = {
            "우주의 신비: 블랙홀의 비밀",
            "은하수 너머의 세계",
            "화성 탐사의 최신 소식"
        };
        
        String[] mockDescriptions = {
            "우주의 가장 신비로운 천체인 블랙홀에 대해 알아봅시다.",
            "우리 은하 너머에 존재하는 놀라운 우주의 모습을 탐험해보세요.",
            "화성 탐사 로버가 전해주는 최신 발견들을 소개합니다."
        };

        Random random = new Random();
        int index = random.nextInt(mockTitles.length);
        
        return formatVideoData(
            mockTitles[index],
            mockDescriptions[index],
            "dQw4w9WgXcQ",
            "우주 채널",
            LocalDateTime.now()
        );
    }
    
    private Map<String, Object> formatVideoData(String title, String description, String videoId, String channelTitle, LocalDateTime publishedAt) {
        Map<String, Object> data = new HashMap<>();
        
        // 영어 제목인 경우 번역
        String translatedTitle = translateTitleIfNeeded(title);
        String translatedDescription = translateTitleIfNeeded(description);
        
        data.put("title", translatedTitle);
        data.put("description", translatedDescription);
        data.put("videoId", videoId);
        data.put("videoUrl", "https://www.youtube.com/watch?v=" + videoId);
        data.put("channelTitle", channelTitle);
        data.put("publishedAt", publishedAt);
        data.put("summary", generateSummary(translatedTitle, translatedDescription));
        data.put("hashtags", generateHashtags(translatedTitle, translatedDescription));
        data.put("content", formatVideoContent(translatedTitle, translatedDescription, videoId, channelTitle, publishedAt));
        
        return data;
    }
    
    private String translateTitleIfNeeded(String text) {
        if (text == null || text.trim().isEmpty()) return text;
        
        if (isEnglishText(text)) {
            String translated = translateWithOpenAI(text);
            return translated != null ? translated : "[해외영상] " + text;
        }
        return text;
    }
    
    private boolean isEnglishText(String text) {
        int englishCount = 0;
        int koreanCount = 0;
        
        for (char c : text.toCharArray()) {
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                englishCount++;
            } else if (c >= '가' && c <= '힣') {
                koreanCount++;
            }
        }
        
        return englishCount > koreanCount;
    }
    
    private String translateWithOpenAI(String englishText) {
        if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
            return null;
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);
            
            String prompt = String.format("""
                다음 영어 텍스트를 자연스럽고 정확한 한국어로 번역해주세요:
                
                "%s"
                
                요구사항:
                - 우주/과학 전문 용어는 정확하게 번역
                - 자연스럽고 읽기 쉬운 한국어로 번역
                - 번역문만 반환 (설명 없이)
                """, englishText);
            
            Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 200,
                "temperature", 0.3
            );
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                "https://api.openai.com/v1/chat/completions", entity, Map.class);
            
            if (response != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String translatedText = (String) message.get("content");
                    log.info("번역 성공: {} -> {}", englishText, translatedText);
                    return translatedText.trim();
                }
            }
        } catch (Exception e) {
            log.warn("번역 실패: {}", englishText, e);
        }
        
        return null;
    }
    
    private String generateSummary(String title, String description) {
        return title.length() > 50 ? title.substring(0, 47) + "..." : title;
    }
    
    private String generateHashtags(String title, String description) {
        if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
            return generateBasicHashtags(title, description);
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);
            
            String content = title + " " + (description != null ? description : "");
            String prompt = String.format("""
                다음 우주 영상 제목과 설명에서 관련 해시태그를 5개 이내로 생성해주세요:
                
                "제목: %s"
                
                요구사항:
                - 한국어 해시태그로 생성 (예: #우주, #블랙홀)
                - 공백으로 구분하여 반환
                - 해시태그만 반환 (설명 없이)
                - 최대 5개
                """, content);
            
            Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 50,
                "temperature", 0.3
            );
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                "https://api.openai.com/v1/chat/completions", entity, Map.class);
            
            if (response != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String hashtags = (String) message.get("content");
                    log.info("해시태그 생성 성공: {}", hashtags.trim());
                    return hashtags.trim();
                }
            }
        } catch (Exception e) {
            log.warn("해시태그 생성 실패, 기본 해시태그 사용: {}", title, e);
        }
        
        return generateBasicHashtags(title, description);
    }
    
    private String generateBasicHashtags(String title, String description) {
        List<String> tags = new ArrayList<>();
        String content = (title + " " + (description != null ? description : "")).toLowerCase();
        
        if (content.contains("우주") || content.contains("space")) tags.add("#우주");
        if (content.contains("블랙홀") || content.contains("blackhole")) tags.add("#블랙홀");
        if (content.contains("화성") || content.contains("mars")) tags.add("#화성");
        if (content.contains("nasa")) tags.add("#NASA");
        if (content.contains("spacex")) tags.add("#SpaceX");
        if (content.contains("다큐멘터리") || content.contains("documentary")) tags.add("#다큐멘터리");
        
        return String.join(" ", tags);
    }
    
    private String formatVideoContent(String title, String description, String videoId, String channelTitle, LocalDateTime publishedAt) {
        StringBuilder content = new StringBuilder();
        
        // 🎬 제목과 요약
        content.append("🎬 **오늘의 우주 영상**: ").append(title).append("\n\n");
        
        if (description != null && !description.trim().isEmpty()) {
            String summary = description.length() > 150 ? description.substring(0, 147) + "..." : description;
            content.append("📌 **요약** ").append(summary).append("\n\n");
        }
        
        // YouTube 비디오 임베드
        content.append("▶️ **영상 보기**\n\n");
        content.append(String.format("""
            <div class="video-container" style="position: relative; padding-bottom: 56.25%%; height: 0; overflow: hidden; max-width: 100%%; background: #000; margin: 20px 0;">
                <iframe src="https://www.youtube.com/embed/%s" 
                        frameborder="0" 
                        allowfullscreen 
                        style="position: absolute; top: 0; left: 0; width: 100%%; height: 100%%;">
                </iframe>
            </div>
            
            """, videoId));
        
        content.append("⚠️ **영상이 보이지 않나요?** [YouTube에서 보기](https://www.youtube.com/watch?v=").append(videoId).append(")\n\n");
        
        // 채널 및 발행일 정보
        content.append("📺 **채널명**: ").append(channelTitle);
        if (publishedAt != null) {
            content.append(" 📅 **발행일**: ").append(publishedAt.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));
        }
        content.append("\n\n");
        
        // 상세 설명
        if (description != null && !description.trim().isEmpty()) {
            content.append("📝 **설명** ").append(description).append("\n\n");
        }
        
        // YouTube 링크
        content.append("🔗 **YouTube 바로가기**\n");
        content.append("[🎬 원본 영상 보기](https://www.youtube.com/watch?v=").append(videoId).append(")\n\n");
        
        content.append("💬 **자유롭게 의견을 나눠주세요!**\n\n");
        content.append("---\n\n");
        
        return content.toString();
    }
    
    private User getSystemUser() {
        return userRepository.findByEmail("newsbot@byeolnight.com")
                .orElseThrow(() -> new RuntimeException("시스템 사용자를 찾을 수 없습니다"));
    }
    
    private LocalDateTime parsePublishedDateTime(String publishedAt) {
        try {
            return LocalDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
    
    public Map<String, Object> getCinemaStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("totalVideos", cinemaRepository.count());
        status.put("recentVideos", cinemaRepository.findTop10ByOrderByCreatedAtDesc().size());
        status.put("lastCollectionTime", LocalDateTime.now());
        status.put("configuration", cinemaConfig);
        return status;
    }
}