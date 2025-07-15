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
        
        // 기본 품질 체크
        if (title.toLowerCase().contains("shorts") || 
            title.toLowerCase().contains("#shorts") ||
            title.length() < cinemaConfig.getQuality().getMinTitleLength() ||
            (description != null && description.length() < cinemaConfig.getQuality().getMinDescriptionLength())) {
            return false;
        }
        
        // 고품질 채널 체크
        for (String channel : cinemaConfig.getYoutube().getQualityChannels()) {
            if (channelTitle.toLowerCase().contains(channel.toLowerCase())) {
                return true;
            }
        }
        
        // 전문 용어 체크
        for (String term : cinemaConfig.getYoutube().getProfessionalTerms()) {
            if (title.toLowerCase().contains(term)) {
                return true;
            }
        }
        
        return false;
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
        
        // YouTube 비디오 임베드
        content.append(String.format("""
            <div class="video-container" style="position: relative; padding-bottom: 56.25%%; height: 0; overflow: hidden; max-width: 100%%; background: #000;">
                <iframe src="https://www.youtube.com/embed/%s" 
                        frameborder="0" 
                        allowfullscreen 
                        style="position: absolute; top: 0; left: 0; width: 100%%; height: 100%%;">
                </iframe>
            </div>
            
            """, videoId));
        
        // 비디오 설명
        if (description != null && !description.trim().isEmpty()) {
            content.append("🎥 **비디오 소개**\n\n");
            content.append(description).append("\n\n");
        }
        
        // 채널 정보
        content.append("📺 **채널**: ").append(channelTitle).append("\n");
        if (publishedAt != null) {
            content.append("📅 **발행일**: ").append(publishedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        
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