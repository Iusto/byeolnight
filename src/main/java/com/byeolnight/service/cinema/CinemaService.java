package com.byeolnight.service.cinema;

import com.byeolnight.entity.Cinema;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.CinemaRepository;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.repository.user.UserRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CinemaService {

    private final CinemaRepository cinemaRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CinemaCollectionProperties cinemaConfig;
    private final RestTemplate restTemplate;
    
    @Value("${app.security.external-api.ai.google-api-key:}")
    private String googleApiKey;
    
    @Value("${app.security.external-api.ai.openai-api-key:}")
    private String openaiApiKey;

    // ================================ 스케줄링 ================================
    
    @Scheduled(cron = "0 0 20 * * *", zone = "Asia/Seoul")
    @Transactional
    public void createDailyCinemaPost() {
        executeWithRetry("일일 자동 포스팅");
    }
    
    @Scheduled(cron = "0 5 20 * * *", zone = "Asia/Seoul")
    @Transactional
    public void retryDailyCinemaPost() {
        if (shouldRetryToday()) {
            executeWithRetry("재시도 포스팅");
        }
    }
    
    @Scheduled(cron = "0 10 20 * * *", zone = "Asia/Seoul")
    @Transactional
    public void finalRetryDailyCinemaPost() {
        if (shouldRetryToday()) {
            executeWithRetry("최종 재시도 포스팅");
        }
    }
    
    private void executeWithRetry(String type) {
        try {
            log.info("별빛 시네마 {} 시작", type);
            collectAndSaveSpaceVideo(getSystemUser());
        } catch (Exception e) {
            log.error("별빛 시네마 {} 실패", type, e);
        }
    }
    
    private boolean shouldRetryToday() {
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        long todayPosts = cinemaRepository.countByCreatedAtAfter(todayStart);
        boolean shouldRetry = todayPosts == 0;
        log.info("오늘 별빛시네마 게시글 수: {}, 재시도 필요: {}", todayPosts, shouldRetry);
        return shouldRetry;
    }

    // ================================ 공개 API ================================
    
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
        
        Map<String, Object> videoData = fetchSpaceVideo();
        if (videoData == null) {
            log.warn("영상 데이터를 가져올 수 없습니다");
            return;
        }
        
        String videoId = (String) videoData.get("videoId");
        String title = (String) videoData.get("title");
        
        if (isDuplicateVideo(videoId, title)) {
            log.info("중복 영상으로 스킵: {}", title);
            return;
        }
        
        Cinema cinema = convertToCinema(videoData);
        cinemaRepository.save(cinema);
        
        Post post = convertToPost(videoData, user);
        Post savedPost = postRepository.save(post);
        
        log.info("새 별빛시네마 게시글 저장: {}", savedPost.getTitle());
    }

    // ================================ YouTube 검색 ================================
    
    private Map<String, Object> fetchSpaceVideo() {
        if (googleApiKey == null || googleApiKey.isEmpty()) {
            return createMockVideoData();
        }

        String[][] keywordSets = {KeywordConstants.KOREAN_KEYWORDS, KeywordConstants.ENGLISH_KEYWORDS};
        
        for (String[] keywords : keywordSets) {
            for (int attempt = 0; attempt < cinemaConfig.getCollection().getRetryCount(); attempt++) {
                try {
                    Map<String, Object> video = searchYouTube(keywords);
                    if (video != null && !isSimilarToExistingVideos(video)) {
                        return video;
                    }
                    Thread.sleep(1000 * (attempt + 1));
                } catch (Exception e) {
                    log.warn("YouTube 검색 시도 {}/{} 실패: {}", attempt + 1, cinemaConfig.getCollection().getRetryCount(), e.getMessage());
                }
            }
        }
        
        return createMockVideoData();
    }
    
    private Map<String, Object> searchYouTube(String[] keywords) {
        String query = getRandomKeywords(keywords, cinemaConfig.getCollection().getKeywordCount());
        
        String url = buildYouTubeSearchUrl(query, "viewCount");
        log.info("YouTube API 호출 (조회수 순): {}", query);
        
        List<Map<String, Object>> qualityVideos = getQualityVideos(url);
        
        if (qualityVideos.isEmpty()) {
            log.warn("고품질 영상을 찾지 못함, 관련도 순으로 재검색");
            return searchYouTubeByRelevance(query);
        }
        
        Map<String, Object> selectedVideo = selectRandomFromTopVideos(qualityVideos);
        logSelectedVideo(selectedVideo);
        
        return parseVideoData(selectedVideo);
    }
    
    private Map<String, Object> searchYouTubeByRelevance(String query) {
        String url = buildYouTubeSearchUrl(query, "relevance");
        List<Map<String, Object>> qualityVideos = getQualityVideos(url);
        
        if (qualityVideos.isEmpty()) return null;
        
        Map<String, Object> selectedVideo = qualityVideos.get(new Random().nextInt(qualityVideos.size()));
        return parseVideoData(selectedVideo);
    }
    
    private String buildYouTubeSearchUrl(String query, String order) {
        return String.format(
            "https://www.googleapis.com/youtube/v3/search?part=snippet&q=%s&type=video&maxResults=%d&order=%s&publishedAfter=%s&videoDuration=%s&videoDefinition=%s&key=%s",
            query, cinemaConfig.getQuality().getMaxResults(), order, getPublishedAfterDate(), 
            cinemaConfig.getQuality().getVideoDuration(), cinemaConfig.getQuality().getVideoDefinition(), googleApiKey
        );
    }
    
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getQualityVideos(String url) {
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null) return List.of();
        
        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
        if (items == null || items.isEmpty()) return List.of();
        
        return items.stream()
            .filter(this::isQualityVideo)
            .map(this::enrichWithVideoStats)
            .filter(Objects::nonNull)
            .filter(this::hasMinimumEngagement)
            .sorted(this::compareVideoQuality)
            .collect(Collectors.toList());
    }
    
    private Map<String, Object> selectRandomFromTopVideos(List<Map<String, Object>> qualityVideos) {
        int topCount = Math.max(1, qualityVideos.size() / 3);
        List<Map<String, Object>> topVideos = qualityVideos.subList(0, topCount);
        return topVideos.get(new Random().nextInt(topVideos.size()));
    }
    
    private void logSelectedVideo(Map<String, Object> selectedVideo) {
        log.info("선택된 영상: {} (조회수: {}, 좋아요: {})", 
                getVideoTitle(selectedVideo), 
                getVideoViewCount(selectedVideo),
                getVideoLikeCount(selectedVideo));
    }

    // ================================ 영상 품질 검증 ================================
    
    private boolean isQualityVideo(Map<String, Object> video) {
        @SuppressWarnings("unchecked")
        Map<String, Object> snippet = (Map<String, Object>) video.get("snippet");
        String title = (String) snippet.get("title");
        String description = (String) snippet.get("description");
        String channelTitle = (String) snippet.get("channelTitle");
        
        if (title == null || description == null) return false;
        
        String titleLower = title.toLowerCase();
        String descLower = description.toLowerCase();
        
        // 기본 품질 체크
        if (!passesBasicQualityCheck(titleLower, title, description)) {
            return false;
        }
        
        // 음악/상업적 콘텐츠 필터링
        if (ContentFilter.isKPopOrMusicContent(titleLower, descLower) || 
            ContentFilter.isCommercialContent(titleLower, descLower)) {
            log.debug("음악/상업적 콘텐츠로 제외: {}", title);
            return false;
        }
        
        // 전문 채널 우선순위
        if (channelTitle != null && isProfessionalChannel(channelTitle)) {
            log.info("고품질 채널 발견: {}", channelTitle);
            return true;
        }
        
        // 우주 콘텐츠 검증
        return ContentValidator.hasValidSpaceContent(titleLower, descLower);
    }
    
    private boolean passesBasicQualityCheck(String titleLower, String title, String description) {
        return !titleLower.contains("shorts") && 
               !titleLower.contains("#shorts") &&
               title.length() >= cinemaConfig.getQuality().getMinTitleLength() &&
               description.length() >= cinemaConfig.getQuality().getMinDescriptionLength();
    }
    
    private boolean isProfessionalChannel(String channelTitle) {
        String channelLower = channelTitle.toLowerCase();
        String[] qualityChannels = cinemaConfig.getYoutube().getQualityChannels();
        
        for (String qualityChannel : qualityChannels) {
            if (channelLower.contains(qualityChannel.toLowerCase())) {
                return true;
            }
        }
        
        return channelLower.contains("science") || 
               channelLower.contains("space") || 
               channelLower.contains("astronomy") ||
               channelLower.contains("documentary") ||
               channelLower.contains("education");
    }

    // ================================ 영상 통계 및 품질 평가 ================================
    
    private Map<String, Object> enrichWithVideoStats(Map<String, Object> video) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> videoId = (Map<String, Object>) video.get("id");
            String id = (String) videoId.get("videoId");
            
            if (id == null) return null;
            
            String statsUrl = String.format(
                "https://www.googleapis.com/youtube/v3/videos?part=statistics,contentDetails&id=%s&key=%s",
                id, googleApiKey
            );
            
            @SuppressWarnings("unchecked")
            Map<String, Object> statsResponse = restTemplate.getForObject(statsUrl, Map.class);
            
            if (statsResponse != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) statsResponse.get("items");
                
                if (items != null && !items.isEmpty()) {
                    Map<String, Object> videoStats = items.get(0);
                    video.put("statistics", videoStats.get("statistics"));
                    video.put("contentDetails", videoStats.get("contentDetails"));
                }
            }
            
            return video;
        } catch (Exception e) {
            log.warn("영상 통계 조회 실패: {}", e.getMessage());
            return video;
        }
    }
    
    private boolean hasMinimumEngagement(Map<String, Object> video) {
        @SuppressWarnings("unchecked")
        Map<String, Object> statistics = (Map<String, Object>) video.get("statistics");
        
        if (statistics == null) return true;
        
        try {
            if (!hasMinimumViews(statistics, video)) return false;
            if (!hasMinimumLikes(statistics, video)) return false;
            return true;
        } catch (NumberFormatException e) {
            return true;
        }
    }
    
    private boolean hasMinimumViews(Map<String, Object> statistics, Map<String, Object> video) {
        String viewCountStr = (String) statistics.get("viewCount");
        if (viewCountStr != null) {
            long viewCount = Long.parseLong(viewCountStr);
            if (viewCount < 10000) {
                log.debug("조회수 부족으로 제외: {} ({}회)", getVideoTitle(video), viewCount);
                return false;
            }
        }
        return true;
    }
    
    private boolean hasMinimumLikes(Map<String, Object> statistics, Map<String, Object> video) {
        String likeCountStr = (String) statistics.get("likeCount");
        if (likeCountStr != null) {
            long likeCount = Long.parseLong(likeCountStr);
            if (likeCount < 100) {
                log.debug("좋아요 부족으로 제외: {} ({}개)", getVideoTitle(video), likeCount);
                return false;
            }
        }
        return true;
    }
    
    private int compareVideoQuality(Map<String, Object> v1, Map<String, Object> v2) {
        // 전문 채널 우선순위
        boolean v1Professional = isProfessionalChannel(getChannelTitle(v1));
        boolean v2Professional = isProfessionalChannel(getChannelTitle(v2));
        
        if (v1Professional != v2Professional) {
            return v1Professional ? -1 : 1;
        }
        
        // 조회수 비교
        long v1Views = getVideoViewCount(v1);
        long v2Views = getVideoViewCount(v2);
        
        if (v1Views != v2Views) {
            return Long.compare(v2Views, v1Views);
        }
        
        // 좋아요 비교
        return Long.compare(getVideoLikeCount(v2), getVideoLikeCount(v1));
    }

    // ================================ 유틸리티 메서드 ================================
    
    private String getRandomKeywords(String[] keywords, int count) {
        Random random = new Random();
        Set<String> selected = new HashSet<>();
        
        while (selected.size() < count && selected.size() < keywords.length) {
            selected.add(keywords[random.nextInt(keywords.length)]);
        }
        
        return String.join(" OR ", selected);
    }
    
    private String getPublishedAfterDate() {
        return LocalDateTime.now()
                .minusYears(cinemaConfig.getYoutube().getPublishedAfterYears())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
    }
    
    private boolean isDuplicateVideo(String videoId, String title) {
        return cinemaRepository.existsByVideoId(videoId) || cinemaRepository.existsByTitle(title);
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

    // ================================ 데이터 변환 ================================
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseVideoData(Map<String, Object> video) {
        Map<String, Object> snippet = (Map<String, Object>) video.get("snippet");
        Map<String, Object> videoId = (Map<String, Object>) video.get("id");
        
        return formatVideoData(
            (String) snippet.get("title"),
            (String) snippet.get("description"),
            (String) videoId.get("videoId"),
            (String) snippet.get("channelTitle"),
            parsePublishedDateTime((String) snippet.get("publishedAt"))
        );
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
    
    private Map<String, Object> formatVideoData(String title, String description, String videoId, String channelTitle, LocalDateTime publishedAt) {
        Map<String, Object> data = new HashMap<>();
        
        String translatedTitle = translateIfNeeded(title);
        String translatedDescription = translateIfNeeded(description);
        
        data.put("title", translatedTitle);
        data.put("description", translatedDescription);
        data.put("videoId", videoId);
        data.put("videoUrl", "https://www.youtube.com/watch?v=" + videoId);
        data.put("channelTitle", channelTitle);
        data.put("publishedAt", publishedAt);
        data.put("summary", generateSummary(translatedTitle));
        data.put("hashtags", generateHashtags(translatedTitle));
        data.put("content", formatVideoContent(translatedTitle, translatedDescription, videoId, channelTitle, publishedAt));
        
        return data;
    }

    // ================================ 번역 및 콘텐츠 생성 ================================
    
    private String translateIfNeeded(String text) {
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
    
    private String generateSummary(String title) {
        return title.length() > 50 ? title.substring(0, 47) + "..." : title;
    }
    
    private String generateHashtags(String title) {
        List<String> tags = new ArrayList<>();
        String content = title.toLowerCase();
        
        if (content.contains("우주") || content.contains("space")) tags.add("#우주");
        if (content.contains("블랙홀") || content.contains("blackhole")) tags.add("#블랙홀");
        if (content.contains("화성") || content.contains("mars")) tags.add("#화성");
        if (content.contains("nasa")) tags.add("#NASA");
        if (content.contains("spacex")) tags.add("#SpaceX");
        
        return String.join(" ", tags);
    }
    
    private String formatVideoContent(String title, String description, String videoId, String channelTitle, LocalDateTime publishedAt) {
        StringBuilder content = new StringBuilder();
        
        content.append("🎬 **오늘의 우주 영상**: ").append(title).append("\n\n");
        
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
        
        content.append("📺 **채널명**: ").append(channelTitle);
        if (publishedAt != null) {
            content.append(" 📅 **발행일**: ").append(publishedAt.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));
        }
        content.append("\n\n");
        
        if (description != null && !description.trim().isEmpty()) {
            content.append("📝 **설명** ").append(description).append("\n\n");
        }

        content.append("💬 **자유롭게 의견을 나눠주세요!**\n\n");
        content.append("---\n\n");
        
        return content.toString();
    }

    // ================================ 헬퍼 메서드 ================================
    
    private String getVideoTitle(Map<String, Object> video) {
        @SuppressWarnings("unchecked")
        Map<String, Object> snippet = (Map<String, Object>) video.get("snippet");
        return snippet != null ? (String) snippet.get("title") : "";
    }
    
    private String getChannelTitle(Map<String, Object> video) {
        @SuppressWarnings("unchecked")
        Map<String, Object> snippet = (Map<String, Object>) video.get("snippet");
        return snippet != null ? (String) snippet.get("channelTitle") : "";
    }
    
    private long getVideoViewCount(Map<String, Object> video) {
        @SuppressWarnings("unchecked")
        Map<String, Object> statistics = (Map<String, Object>) video.get("statistics");
        if (statistics != null) {
            String viewCountStr = (String) statistics.get("viewCount");
            if (viewCountStr != null) {
                try {
                    return Long.parseLong(viewCountStr);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }
    
    private long getVideoLikeCount(Map<String, Object> video) {
        @SuppressWarnings("unchecked")
        Map<String, Object> statistics = (Map<String, Object>) video.get("statistics");
        if (statistics != null) {
            String likeCountStr = (String) statistics.get("likeCount");
            if (likeCountStr != null) {
                try {
                    return Long.parseLong(likeCountStr);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
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

    // ================================ 공개 API (기존 호환성) ================================
    
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchSpaceVideos() {
        try {
            String url = String.format(
                "https://www.googleapis.com/youtube/v3/search?part=snippet&q=%s&type=video&maxResults=%d&order=relevance&regionCode=KR&relevanceLanguage=ko&key=%s",
                getRandomSpaceQuery(), 12, googleApiKey
            );
            
            log.info("YouTube API 호출: 우주 관련 영상 검색");
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null) {
                var items = (List<Map<String, Object>>) response.get("items");
                log.info("YouTube 영상 검색 성공: {}개", items != null ? items.size() : 0);
                return items != null ? items : List.of();
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
            String url = String.format(
                "https://www.googleapis.com/youtube/v3/search?part=snippet&q=%s 우주&type=video&maxResults=%d&order=relevance&regionCode=KR&relevanceLanguage=ko&key=%s",
                keyword, 6, googleApiKey
            );
            
            log.info("YouTube API 호출: {} 관련 영상 검색", keyword);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null) {
                var items = (List<Map<String, Object>>) response.get("items");
                return items != null ? items : List.of();
            }
            
            return List.of();
            
        } catch (Exception e) {
            log.error("YouTube 키워드 검색 중 오류 발생: {}", keyword, e);
            return List.of();
        }
    }
    
    public List<Map<String, Object>> getUniqueSpaceVideos() {
        List<Map<String, Object>> allVideos = new ArrayList<>();
        Set<String> videoIds = new HashSet<>();
        
        for (int i = 0; i < 3; i++) {
            List<Map<String, Object>> videos = searchSpaceVideos();
            for (Map<String, Object> video : videos) {
                @SuppressWarnings("unchecked")
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
    
    private String getRandomSpaceQuery() {
        Random random = new Random();
        Set<String> selectedKeywords = new HashSet<>();
        
        while (selectedKeywords.size() < 3 && selectedKeywords.size() < KeywordConstants.KOREAN_KEYWORDS.length) {
            int randomIndex = random.nextInt(KeywordConstants.KOREAN_KEYWORDS.length);
            selectedKeywords.add(KeywordConstants.KOREAN_KEYWORDS[randomIndex]);
        }
        
        String query = String.join(" ", selectedKeywords);
        log.info("YouTube 검색 키워드: {}", query);
        return query;
    }
    
    public Map<String, Object> getCinemaStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            long totalCinemaPosts = postRepository.countByCategory(Post.Category.STARLIGHT_CINEMA);
            status.put("totalCinemaPosts", totalCinemaPosts);
            
            Optional<Post> latestPost = postRepository.findFirstByCategoryOrderByCreatedAtDesc(Post.Category.STARLIGHT_CINEMA);
            boolean latestPostExists = latestPost.isPresent();
            status.put("latestPostExists", latestPostExists);
            
            if (latestPostExists) {
                Post latest = latestPost.get();
                status.put("latestPostTitle", latest.getTitle());
                status.put("lastUpdated", latest.getCreatedAt());
                
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime lastUpdate = latest.getCreatedAt();
                long daysSinceUpdate = java.time.temporal.ChronoUnit.DAYS.between(lastUpdate, now);
                status.put("daysSinceLastUpdate", daysSinceUpdate);
                
                boolean isHealthy = daysSinceUpdate < 2;
                status.put("systemHealthy", isHealthy);
                
                if (!isHealthy) {
                    status.put("warning", "마지막 업데이트가 " + daysSinceUpdate + "일 전입니다. 스케줄러 확인이 필요합니다.");
                }
            } else {
                status.put("latestPostTitle", null);
                status.put("lastUpdated", null);
                status.put("daysSinceLastUpdate", -1);
                status.put("systemHealthy", false);
                status.put("warning", "별빛 시네마 게시글이 없습니다.");
            }
            
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            long todayPosts = postRepository.countByCategoryAndCreatedAtAfter(Post.Category.STARLIGHT_CINEMA, todayStart);
            status.put("todayPosts", todayPosts);
            
            boolean googleApiConfigured = googleApiKey != null && !googleApiKey.trim().isEmpty();
            boolean openaiApiConfigured = openaiApiKey != null && !openaiApiKey.trim().isEmpty();
            
            status.put("googleApiConfigured", googleApiConfigured);
            status.put("openaiApiConfigured", openaiApiConfigured);
            
            Map<String, Object> systemConfig = new HashMap<>();
            systemConfig.put("schedulerEnabled", true);
            systemConfig.put("dailyScheduleTime", "20:00 (KST)");
            systemConfig.put("retryTimes", "20:05, 20:10 (KST)");
            systemConfig.put("maxRetryCount", cinemaConfig.getCollection().getRetryCount());
            systemConfig.put("keywordCount", cinemaConfig.getCollection().getKeywordCount());
            status.put("systemConfig", systemConfig);
            
            if (totalCinemaPosts == 0) {
                status.put("statusMessage", "별빛 시네마 시스템이 아직 시작되지 않았습니다.");
            } else if (!latestPostExists) {
                status.put("statusMessage", "별빛 시네마 게시글을 찾을 수 없습니다.");
            } else if (status.get("daysSinceLastUpdate") != null && (Long) status.get("daysSinceLastUpdate") >= 2) {
                status.put("statusMessage", "별빛 시네마 시스템에 주의가 필요합니다.");
            } else {
                status.put("statusMessage", "별빛 시네마 시스템이 정상 작동 중입니다.");
            }
            
        } catch (Exception e) {
            log.error("별빛 시네마 상태 조회 실패", e);
            status.put("error", "상태 조회 실패: " + e.getMessage());
            status.put("systemHealthy", false);
            status.put("statusMessage", "시스템 상태를 확인할 수 없습니다.");
        }
        
        return status;
    }

    // ================================ 내부 클래스 ================================
    
    private static class KeywordConstants {
        static final String[] KOREAN_KEYWORDS = {"우주", "로켓", "위성", "화성", "달", "태양", "지구", "목성", "토성", "천왕성", "해왕성", "수성", "금성", "명왕성", "블랙홀", "은하", "별", "항성", "혜성", "소행성", "망원경", "천문", "항공우주", "우주선", "우주정거장", "우주비행사", "우주발사", "우주탐사", "성운", "퀘이사", "중성자별", "백색왜성", "적색거성", "초신성", "성단", "성간물질", "암흑물질", "암흑에너지", "빅뱅", "우주론", "외계행성", "외계생명", "SETI", "우주망원경", "허블", "제임스웹", "케플러", "스피처", "찬드라", "컴프턴", "국제우주정거장", "ISS", "아르테미스", "아폴로", "보이저", "카시니", "갈릴레오", "뉴호라이즌스", "파커", "주노", "화성탐사", "달탐사", "목성탐사", "토성탐사", "태양탐사", "소행성탐사", "혜성탐사", "우주쓰레기", "우주날씨", "태양풍", "자기권", "오로라", "일식", "월식", "유성우", "운석", "크레이터", "화산", "대기", "중력", "궤도", "공전", "자전", "조석", "라그랑주점", "중력파", "상대성이론", "양자역학", "끈이론", "다중우주", "우주배경복사", "적색편이", "도플러효과", "허블상수", "우주나이", "우주크기", "관측가능우주", "사건지평선", "특이점", "웜홀"};
        static final String[] ENGLISH_KEYWORDS = {"space", "rocket", "satellite", "Mars", "Moon", "Sun", "Earth", "Jupiter", "Saturn", "Uranus", "Neptune", "Mercury", "Venus", "Pluto", "blackhole", "galaxy", "star", "stellar", "comet", "asteroid", "telescope", "astronomy", "aerospace", "spacecraft", "space station", "astronaut", "space launch", "space exploration", "nebula", "quasar", "neutron star", "white dwarf", "red giant", "supernova", "cluster", "interstellar", "dark matter", "dark energy", "big bang", "cosmology", "exoplanet", "extraterrestrial", "SETI", "space telescope", "Hubble", "James Webb", "Kepler", "Spitzer", "Chandra", "Compton", "ISS", "International Space Station", "Artemis", "Apollo", "Voyager", "Cassini", "Galileo", "New Horizons", "Parker", "Juno", "Mars exploration", "lunar exploration", "Jupiter mission", "Saturn mission", "solar mission", "asteroid mission", "comet mission", "space debris", "space weather", "solar wind", "magnetosphere", "aurora", "eclipse", "lunar eclipse", "meteor shower", "meteorite", "crater", "volcano", "atmosphere", "gravity", "orbit", "revolution", "rotation", "tidal", "Lagrange point", "gravitational wave", "relativity", "quantum mechanics", "string theory", "multiverse", "cosmic background", "redshift", "Doppler effect", "Hubble constant", "universe age", "universe size", "observable universe", "event horizon", "singularity", "wormhole"};
    }
    
    private static class ContentFilter {
        private static final String[] MUSIC_KEYWORDS = {"원위", "onewe", "bts", "blackpink", "twice", "red velvet", "aespa", "itzy", "ive", "newjeans", "stray kids", "seventeen", "nct", "exo", "bigbang", "2ne1", "girls generation", "snsd", "더 쇼", "the show", "music bank", "inkigayo", "m countdown", "show champion", "뮤직뱅크", "인기가요", "엠카운트다운", "쇼챔피언", "음악중심", "music core", "comeback", "컴백", "debut", "데뷔", "mv", "뮤직비디오", "music video", "live stage", "라이브", "performance", "퍼포먼스", "dance practice", "안무", "idol", "아이돌", "kpop", "k-pop", "케이팝", "한류", "hallyu", "가사", "lyrics", "노래", "song", "음악", "music", "앨범", "album", "미발매", "unreleased", "콘서트", "concert", "페스티벌", "festival", "칸타빌레", "cantabile", "더 시즌즈", "the seasons", "박보검", "샘 킴", "sam kim", "오현우", "ohHyunwoo", "일식", "eclipse", "[가사]", "[lyrics]", "kbs", "방송", "태양의 후예", "descendants of the sun", "ost", "사운드트랙", "soundtrack", "드라마", "drama", "영화", "movie", "시네마", "cinema", "배우", "actor", "actress", "여배우", "가수", "singer", "아티스트", "artist", "뮤지션", "musician", "밴드", "band", "그룹", "group", "솔로", "solo", "듀엣", "duet", "트리오", "trio", "보컬", "vocal", "래퍼", "rapper", "댄서", "dancer", "프로듀서", "producer", "작곡가", "composer", "작사가", "lyricist"};
        
        private static final String[] COMMERCIAL_KEYWORDS = {"쇼핑", "shopping", "구매", "buy", "판매", "sale", "할인", "discount", "특가", "세일", "광고", "ad", "advertisement", "홍보", "promotion", "캠페인", "campaign", "브랜드", "brand", "제품", "product", "상품", "item", "리뷰", "review", "언박싱", "unboxing", "추천", "recommend", "후기", "testimonial", "체험", "experience", "협찬", "sponsored", "파트너십", "partnership", "마케팅", "marketing"};
        
        static boolean isKPopOrMusicContent(String titleLower, String descLower) {
            return Arrays.stream(MUSIC_KEYWORDS)
                    .anyMatch(keyword -> titleLower.contains(keyword) || descLower.contains(keyword));
        }
        
        static boolean isCommercialContent(String titleLower, String descLower) {
            return Arrays.stream(COMMERCIAL_KEYWORDS)
                    .anyMatch(keyword -> titleLower.contains(keyword) || descLower.contains(keyword));
        }
    }
    
    private static class ContentValidator {
        static boolean hasValidSpaceContent(String titleLower, String descLower) {
            int spaceKeywordCount = 0;
            List<String> foundKeywords = new ArrayList<>();
            
            for (String keyword : KeywordConstants.KOREAN_KEYWORDS) {
                if (titleLower.contains(keyword.toLowerCase()) || descLower.contains(keyword.toLowerCase())) {
                    spaceKeywordCount++;
                    foundKeywords.add(keyword);
                }
            }
            
            for (String keyword : KeywordConstants.ENGLISH_KEYWORDS) {
                if (titleLower.contains(keyword.toLowerCase()) || descLower.contains(keyword.toLowerCase())) {
                    spaceKeywordCount++;
                    foundKeywords.add(keyword);
                }
            }
            
            if (spaceKeywordCount < 2) {
                return false;
            }
            
            // "태양" 키워드 특별 처리
            if (foundKeywords.contains("태양") || foundKeywords.contains("sun")) {
                if (titleLower.contains("태양의") || titleLower.contains("descendants")) {
                    return false;
                }
                
                boolean hasOtherSpaceKeywords = foundKeywords.stream()
                        .anyMatch(k -> !k.equals("태양") && !k.equals("sun"));
                
                if (!hasOtherSpaceKeywords) {
                    return false;
                }
            }
            
            // 전문적인 우주 키워드 우선 체크
            String[] professionalKeywords = {
                "블랙홀", "blackhole", "중성자별", "neutron star", 
                "초신성", "supernova", "우주망원경", "space telescope",
                "허블", "hubble", "제임스웹", "james webb", "nasa", "spacex",
                "화성탐사", "mars exploration", "달탐사", "lunar exploration"
            };
            
            boolean hasProfessionalKeyword = Arrays.stream(professionalKeywords)
                    .anyMatch(k -> titleLower.contains(k) || descLower.contains(k));
            
            if (hasProfessionalKeyword) {
                return true;
            }
            
            return spaceKeywordCount >= 3;
        }
    }
}