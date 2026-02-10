package com.byeolnight.service.cinema;

import com.byeolnight.dto.external.openai.OpenAiChatRequest;
import com.byeolnight.dto.external.openai.OpenAiChatResponse;
import com.byeolnight.dto.external.openai.OpenAiMessage;
import com.byeolnight.dto.external.youtube.*;
import com.byeolnight.dto.video.VideoDto;
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
    
    @Value("${app.system.users.newsbot.email:newsbot@byeolnight.com}")
    private String newsbotEmail;

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

        List<YouTubeVideoItem> qualityVideos = getQualityVideos(url);

        if (qualityVideos.isEmpty()) {
            log.warn("고품질 영상을 찾지 못함, 관련도 순으로 재검색");
            return searchYouTubeByRelevance(query);
        }

        YouTubeVideoItem selectedVideo = selectRandomFromTopVideos(qualityVideos);
        logSelectedVideo(selectedVideo);

        return parseVideoData(selectedVideo);
    }

    private Map<String, Object> searchYouTubeByRelevance(String query) {
        String url = buildYouTubeSearchUrl(query, "relevance");
        List<YouTubeVideoItem> qualityVideos = getQualityVideos(url);

        if (qualityVideos.isEmpty()) return null;

        YouTubeVideoItem selectedVideo = qualityVideos.get(new Random().nextInt(qualityVideos.size()));
        return parseVideoData(selectedVideo);
    }

    private String buildYouTubeSearchUrl(String query, String order) {
        return String.format(
            "https://www.googleapis.com/youtube/v3/search?part=snippet&q=%s&type=video&maxResults=%d&order=%s&publishedAfter=%s&videoDuration=%s&videoDefinition=%s&key=%s",
            query, cinemaConfig.getQuality().getMaxResults(), order, getPublishedAfterDate(),
            cinemaConfig.getQuality().getVideoDuration(), cinemaConfig.getQuality().getVideoDefinition(), googleApiKey
        );
    }

    private List<YouTubeVideoItem> getQualityVideos(String url) {
        YouTubeSearchResponse response = restTemplate.getForObject(url, YouTubeSearchResponse.class);
        if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
            return List.of();
        }

        return response.getItems().stream()
            .filter(this::isQualityVideo)
            .map(this::enrichWithVideoStats)
            .filter(Objects::nonNull)
            .filter(this::hasMinimumEngagement)
            .sorted(this::compareVideoQuality)
            .collect(Collectors.toList());
    }

    private YouTubeVideoItem selectRandomFromTopVideos(List<YouTubeVideoItem> qualityVideos) {
        int topCount = Math.max(1, qualityVideos.size() / 3);
        List<YouTubeVideoItem> topVideos = qualityVideos.subList(0, topCount);
        return topVideos.get(new Random().nextInt(topVideos.size()));
    }

    private void logSelectedVideo(YouTubeVideoItem selectedVideo) {
        YouTubeStatistics stats = selectedVideo.getStatistics();
        log.info("선택된 영상: {} (조회수: {}, 좋아요: {})",
                selectedVideo.getSnippet().getTitle(),
                stats != null ? stats.getViewCountAsLong() : 0,
                stats != null ? stats.getLikeCountAsLong() : 0);
    }

    // ================================ 영상 품질 검증 ================================
    
    private boolean isQualityVideo(YouTubeVideoItem video) {
        YouTubeSnippet snippet = video.getSnippet();
        if (snippet == null) return false;

        String title = snippet.getTitle();
        String description = snippet.getDescription();
        String channelTitle = snippet.getChannelTitle();

        if (title == null || description == null) return false;

        String titleLower = title.toLowerCase();
        String descLower = description.toLowerCase();

        // 기본 품질 체크
        if (!passesBasicQualityCheck(titleLower, title, description)) {
            return false;
        }

        // 음악/상업적/드라마 콘텐츠 필터링
        if (ContentFilter.isKPopOrMusicContent(titleLower, descLower) ||
            ContentFilter.isCommercialContent(titleLower, descLower) ||
            ContentFilter.isDramaOrEntertainmentContent(titleLower, descLower)) {
            log.debug("음악/상업적/드라마 콘텐츠로 제외: {}", title);
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
    
    private YouTubeVideoItem enrichWithVideoStats(YouTubeVideoItem video) {
        try {
            YouTubeVideoId videoId = video.getId();
            if (videoId == null || videoId.getVideoId() == null) return null;

            String statsUrl = String.format(
                "https://www.googleapis.com/youtube/v3/videos?part=statistics,contentDetails&id=%s&key=%s",
                videoId.getVideoId(), googleApiKey
            );

            YouTubeVideoListResponse statsResponse = restTemplate.getForObject(statsUrl, YouTubeVideoListResponse.class);

            if (statsResponse != null && statsResponse.getItems() != null && !statsResponse.getItems().isEmpty()) {
                video.setStatistics(statsResponse.getItems().get(0).getStatistics());
            }

            return video;
        } catch (Exception e) {
            log.warn("영상 통계 조회 실패: {}", e.getMessage());
            return video;
        }
    }

    private boolean hasMinimumEngagement(YouTubeVideoItem video) {
        YouTubeStatistics statistics = video.getStatistics();
        if (statistics == null) return true;

        try {
            long viewCount = statistics.getViewCountAsLong();
            if (viewCount > 0 && viewCount < 10000) {
                log.debug("조회수 부족으로 제외: {} ({}회)", video.getSnippet().getTitle(), viewCount);
                return false;
            }

            long likeCount = statistics.getLikeCountAsLong();
            if (likeCount > 0 && likeCount < 100) {
                log.debug("좋아요 부족으로 제외: {} ({}개)", video.getSnippet().getTitle(), likeCount);
                return false;
            }

            return true;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private int compareVideoQuality(YouTubeVideoItem v1, YouTubeVideoItem v2) {
        // 전문 채널 우선순위
        boolean v1Professional = isProfessionalChannel(v1.getSnippet().getChannelTitle());
        boolean v2Professional = isProfessionalChannel(v2.getSnippet().getChannelTitle());

        if (v1Professional != v2Professional) {
            return v1Professional ? -1 : 1;
        }

        // 조회수 비교
        long v1Views = v1.getStatistics() != null ? v1.getStatistics().getViewCountAsLong() : 0;
        long v2Views = v2.getStatistics() != null ? v2.getStatistics().getViewCountAsLong() : 0;

        if (v1Views != v2Views) {
            return Long.compare(v2Views, v1Views);
        }

        // 좋아요 비교
        long v1Likes = v1.getStatistics() != null ? v1.getStatistics().getLikeCountAsLong() : 0;
        long v2Likes = v2.getStatistics() != null ? v2.getStatistics().getLikeCountAsLong() : 0;
        return Long.compare(v2Likes, v1Likes);
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
    
    private Map<String, Object> parseVideoData(YouTubeVideoItem video) {
        YouTubeSnippet snippet = video.getSnippet();
        YouTubeVideoId videoId = video.getId();

        return formatVideoData(
            snippet.getTitle(),
            snippet.getDescription(),
            videoId.getVideoId(),
            snippet.getChannelTitle(),
            parsePublishedDateTime(snippet.getPublishedAt())
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
            
            OpenAiChatRequest requestBody = OpenAiChatRequest.builder()
                .model("gpt-4o-mini")
                .messages(List.of(OpenAiMessage.user(prompt)))
                .maxTokens(200)
                .temperature(0.3)
                .build();

            HttpEntity<OpenAiChatRequest> entity = new HttpEntity<>(requestBody, headers);
            OpenAiChatResponse response = restTemplate.postForObject(
                "https://api.openai.com/v1/chat/completions", entity, OpenAiChatResponse.class);

            if (response != null) {
                String translatedText = response.getFirstContent();
                if (translatedText != null) {
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
        content.append("https://www.youtube.com/watch?v=").append(videoId).append("\n\n");
        
        content.append("📺 **채널명**: ").append(channelTitle);
        if (publishedAt != null) {
            content.append("\n📅 **발행일**: ").append(publishedAt.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));
        }
        content.append("\n\n");
        
        if (description != null && !description.trim().isEmpty()) {
            String truncatedDesc = description.length() > 200 ? description.substring(0, 197) + "..." : description;
            content.append("📝 **설명**\n").append(truncatedDesc).append("\n\n");
        }

        content.append("💬 **자유롭게 의견을 나눠주세요!**\n\n");
        content.append("---\n");
        
        return content.toString();
    }

    // ================================ 헬퍼 메서드 ================================
    
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
        return userRepository.findByEmail(newsbotEmail)
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

    public List<VideoDto> searchSpaceVideos() {
        try {
            String url = String.format(
                "https://www.googleapis.com/youtube/v3/search?part=snippet&q=%s&type=video&maxResults=%d&order=relevance&regionCode=KR&relevanceLanguage=ko&key=%s",
                getRandomSpaceQuery(), 12, googleApiKey
            );

            log.info("YouTube API 호출: 우주 관련 영상 검색");

            YouTubeSearchResponse response = restTemplate.getForObject(url, YouTubeSearchResponse.class);

            if (response != null && response.getItems() != null) {
                log.info("YouTube 영상 검색 성공: {}개", response.getItems().size());
                return convertToVideoDtoList(response.getItems());
            }

            log.warn("YouTube API 호출 실패");
            return List.of();

        } catch (Exception e) {
            log.error("YouTube 영상 검색 중 오류 발생", e);
            return List.of();
        }
    }

    public List<VideoDto> searchVideosByKeyword(String keyword) {
        try {
            String url = String.format(
                "https://www.googleapis.com/youtube/v3/search?part=snippet&q=%s 우주&type=video&maxResults=%d&order=relevance&regionCode=KR&relevanceLanguage=ko&key=%s",
                keyword, 6, googleApiKey
            );

            log.info("YouTube API 호출: {} 관련 영상 검색", keyword);

            YouTubeSearchResponse response = restTemplate.getForObject(url, YouTubeSearchResponse.class);

            if (response != null && response.getItems() != null) {
                return convertToVideoDtoList(response.getItems());
            }

            return List.of();

        } catch (Exception e) {
            log.error("YouTube 키워드 검색 중 오류 발생: {}", keyword, e);
            return List.of();
        }
    }

    public List<VideoDto> getUniqueSpaceVideos() {
        List<VideoDto> allVideos = new ArrayList<>();
        Set<String> videoIds = new HashSet<>();

        for (int i = 0; i < 3; i++) {
            List<VideoDto> videos = searchSpaceVideos();
            for (VideoDto video : videos) {
                String videoId = video.getVideoId();
                if (videoId != null && !videoIds.contains(videoId)) {
                    videoIds.add(videoId);
                    allVideos.add(video);
                }
            }
        }

        log.info("중복 제거 후 YouTube 영상: {}개", allVideos.size());
        return allVideos;
    }

    private List<VideoDto> convertToVideoDtoList(List<YouTubeVideoItem> items) {
        return items.stream()
            .map(this::convertToVideoDto)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private VideoDto convertToVideoDto(YouTubeVideoItem item) {
        try {
            YouTubeVideoId id = item.getId();
            YouTubeSnippet snippet = item.getSnippet();

            if (id == null || snippet == null) return null;

            String videoId = id.getVideoId();
            if (videoId == null) return null;

            String thumbnailUrl = snippet.getThumbnails() != null ? snippet.getThumbnails().getBestUrl() : null;

            return VideoDto.builder()
                .videoId(videoId)
                .title(snippet.getTitle())
                .description(snippet.getDescription())
                .thumbnailUrl(thumbnailUrl)
                .publishedAt(snippet.getPublishedAt())
                .channelTitle(snippet.getChannelTitle())
                .build();
        } catch (Exception e) {
            log.warn("VideoDto 변환 실패: {}", e.getMessage());
            return null;
        }
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
    
    public com.byeolnight.dto.admin.CinemaStatusDto getCinemaStatus() {
        try {
            long totalCinemaPosts = postRepository.countByCategory(Post.Category.STARLIGHT_CINEMA);
            Optional<Post> latestPost = postRepository.findFirstByCategoryOrderByCreatedAtDesc(Post.Category.STARLIGHT_CINEMA);
            
            com.byeolnight.dto.admin.CinemaStatusDto.CinemaStatusDtoBuilder builder = com.byeolnight.dto.admin.CinemaStatusDto.builder()
                .totalCinemaPosts(totalCinemaPosts)
                .latestPostExists(latestPost.isPresent());
            
            if (latestPost.isPresent()) {
                Post latest = latestPost.get();
                LocalDateTime now = LocalDateTime.now();
                long daysSinceUpdate = java.time.temporal.ChronoUnit.DAYS.between(latest.getCreatedAt(), now);
                boolean isHealthy = daysSinceUpdate < 2;
                
                builder.latestPostTitle(latest.getTitle())
                    .lastUpdated(latest.getCreatedAt())
                    .daysSinceLastUpdate(daysSinceUpdate)
                    .systemHealthy(isHealthy);
                
                if (!isHealthy) {
                    builder.warning("마지막 업데이트가 " + daysSinceUpdate + "일 전입니다. 스케줄러 확인이 필요합니다.");
                }
            } else {
                builder.daysSinceLastUpdate(-1L)
                    .systemHealthy(false)
                    .warning("별빛 시네마 게시글이 없습니다.");
            }
            
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            long todayPosts = postRepository.countByCategoryAndCreatedAtAfter(Post.Category.STARLIGHT_CINEMA, todayStart);
            
            com.byeolnight.dto.admin.CinemaStatusDto.SystemConfigDto systemConfig = com.byeolnight.dto.admin.CinemaStatusDto.SystemConfigDto.builder()
                .schedulerEnabled(true)
                .dailyScheduleTime("20:00 (KST)")
                .retryTimes("20:05, 20:10 (KST)")
                .maxRetryCount(cinemaConfig.getCollection().getRetryCount())
                .keywordCount(cinemaConfig.getCollection().getKeywordCount())
                .build();
            
            String statusMessage;
            if (totalCinemaPosts == 0) {
                statusMessage = "별빛 시네마 시스템이 아직 시작되지 않았습니다.";
            } else if (!latestPost.isPresent()) {
                statusMessage = "별빛 시네마 게시글을 찾을 수 없습니다.";
            } else if (builder.build().getDaysSinceLastUpdate() >= 2) {
                statusMessage = "별빛 시네마 시스템에 주의가 필요합니다.";
            } else {
                statusMessage = "별빛 시네마 시스템이 정상 작동 중입니다.";
            }
            
            return builder
                .todayPosts(todayPosts)
                .googleApiConfigured(googleApiKey != null && !googleApiKey.trim().isEmpty())
                .openaiApiConfigured(openaiApiKey != null && !openaiApiKey.trim().isEmpty())
                .systemConfig(systemConfig)
                .statusMessage(statusMessage)
                .build();
            
        } catch (Exception e) {
            log.error("별빛 시네마 상태 조회 실패", e);
            return com.byeolnight.dto.admin.CinemaStatusDto.builder()
                .error("상태 조회 실패: " + e.getMessage())
                .systemHealthy(false)
                .statusMessage("시스템 상태를 확인할 수 없습니다.")
                .build();
        }
    }

    // ================================ 내부 클래스 ================================
    
    private static class KeywordConstants {
        static final String[] KOREAN_KEYWORDS = {"우주", "로켓", "위성", "화성", "달", "태양", "지구", "목성", "토성", "천왕성", "해왕성", "수성", "금성", "명왕성", "블랙홀", "은하", "별", "항성", "혜성", "소행성", "망원경", "천문", "항공우주", "우주선", "우주정거장", "우주비행사", "우주발사", "우주탐사", "성운", "퀘이사", "중성자별", "백색왜성", "적색거성", "초신성", "성단", "성간물질", "암흑물질", "암흑에너지", "빅뱅", "우주론", "외계행성", "외계생명", "SETI", "우주망원경", "허블", "제임스웹", "케플러", "스피처", "찬드라", "컴프턴", "국제우주정거장", "ISS", "아르테미스", "아폴로", "보이저", "카시니", "갈릴레오", "뉴호라이즌스", "파커", "주노", "화성탐사", "달탐사", "목성탐사", "토성탐사", "태양탐사", "소행성탐사", "혜성탐사", "우주쓰레기", "우주날씨", "태양풍", "자기권", "오로라", "일식", "월식", "유성우", "운석", "크레이터", "화산", "대기", "중력", "궤도", "공전", "자전", "조석", "라그랑주점", "중력파", "상대성이론", "양자역학", "끈이론", "다중우주", "우주배경복사", "적색편이", "도플러효과", "허블상수", "우주나이", "우주크기", "관측가능우주", "사건지평선", "특이점", "웜홀"};
        static final String[] ENGLISH_KEYWORDS = {"space", "rocket", "satellite", "Mars", "Moon", "Sun", "Earth", "Jupiter", "Saturn", "Uranus", "Neptune", "Mercury", "Venus", "Pluto", "blackhole", "galaxy", "star", "stellar", "comet", "asteroid", "telescope", "astronomy", "aerospace", "spacecraft", "space station", "astronaut", "space launch", "space exploration", "nebula", "quasar", "neutron star", "white dwarf", "red giant", "supernova", "cluster", "interstellar", "dark matter", "dark energy", "big bang", "cosmology", "exoplanet", "extraterrestrial", "SETI", "space telescope", "Hubble", "James Webb", "Kepler", "Spitzer", "Chandra", "Compton", "ISS", "International Space Station", "Artemis", "Apollo", "Voyager", "Cassini", "Galileo", "New Horizons", "Parker", "Juno", "Mars exploration", "lunar exploration", "Jupiter mission", "Saturn mission", "solar mission", "asteroid mission", "comet mission", "space debris", "space weather", "solar wind", "magnetosphere", "aurora", "eclipse", "lunar eclipse", "meteor shower", "meteorite", "crater", "volcano", "atmosphere", "gravity", "orbit", "revolution", "rotation", "tidal", "Lagrange point", "gravitational wave", "relativity", "quantum mechanics", "string theory", "multiverse", "cosmic background", "redshift", "Doppler effect", "Hubble constant", "universe age", "universe size", "observable universe", "event horizon", "singularity", "wormhole"};
    }
    
    private static class ContentFilter {
        private static final String[] MUSIC_KEYWORDS = {"원위", "onewe", "bts", "blackpink", "twice", "red velvet", "aespa", "itzy", "ive", "newjeans", "stray kids", "seventeen", "nct", "exo", "bigbang", "2ne1", "girls generation", "snsd", "더 쇼", "the show", "music bank", "inkigayo", "m countdown", "show champion", "뮤직뱅크", "인기가요", "엠카운트다운", "쇼챔피언", "음악중심", "music core", "comeback", "컴백", "debut", "데뷔", "mv", "뮤직비디오", "music video", "live stage", "라이브", "performance", "퍼포먼스", "dance practice", "안무", "idol", "아이돌", "kpop", "k-pop", "케이팝", "한류", "hallyu", "가사", "lyrics", "노래", "song", "음악", "music", "앨범", "album", "미발매", "unreleased", "콘서트", "concert", "페스티벌", "festival", "칸타빌레", "cantabile", "더 시즌즈", "the seasons", "박보검", "샘 킴", "sam kim", "오현우", "ohHyunwoo", "일식", "eclipse", "[가사]", "[lyrics]", "kbs", "방송", "태양의 후예", "descendants of the sun", "ost", "사운드트랙", "soundtrack", "드라마", "drama", "영화", "movie", "시네마", "cinema", "배우", "actor", "actress", "여배우", "가수", "singer", "아티스트", "artist", "뮤지션", "musician", "밴드", "band", "그룹", "group", "솔로", "solo", "듀엣", "duet", "트리오", "trio", "보컬", "vocal", "래퍼", "rapper", "댄서", "dancer", "프로듀서", "producer", "작곡가", "composer", "작사가", "lyricist"};
        
        private static final String[] COMMERCIAL_KEYWORDS = {"쇼핑", "shopping", "구매", "buy", "판매", "sale", "할인", "discount", "특가", "세일", "광고", "ad", "advertisement", "홍보", "promotion", "캠페인", "campaign", "브랜드", "brand", "제품", "product", "상품", "item", "리뷰", "review", "언박싱", "unboxing", "추천", "recommend", "후기", "testimonial", "체험", "experience", "협찬", "sponsored", "파트너십", "partnership", "마케팅", "marketing"};
        
        private static final String[] DRAMA_ENTERTAINMENT_KEYWORDS = {
            // 드라마 관련
            "이 사랑에 이름을 붙인다면", "iss pyaar ko kya naam doon", "아르나브", "arnav", "쿠시", "khushi", 
            "키스", "kiss", "로맨스", "romance", "사랑", "love story", "연애", "relationship", 
            "시즌", "season", "에피소드", "episode", "시리즈", "series", "드라마", "drama", 
            "생일 서프라이즈", "birthday surprise", "결혼", "wedding", "신혼", "honeymoon",
            
            // 인도 드라마/엔터테인먼트
            "bollywood", "볼리우드", "hindi", "힌디", "indian", "인도", "telugu", "tamil", 
            "zee tv", "star plus", "colors tv", "sony tv", "hotstar", "voot", 
            
            // 일반 엔터테인먼트
            "예능", "variety", "토크쇼", "talk show", "리얼리티", "reality", "게임쇼", "game show",
            "인터뷰", "interview", "behind the scenes", "비하인드", "메이킹", "making",
            "셀럽", "celebrity", "스타", "star", "팬미팅", "fan meeting", "팬사인회", "fan sign"
        };
        
        static boolean isKPopOrMusicContent(String titleLower, String descLower) {
            return Arrays.stream(MUSIC_KEYWORDS)
                    .anyMatch(keyword -> titleLower.contains(keyword) || descLower.contains(keyword));
        }
        
        static boolean isCommercialContent(String titleLower, String descLower) {
            return Arrays.stream(COMMERCIAL_KEYWORDS)
                    .anyMatch(keyword -> titleLower.contains(keyword) || descLower.contains(keyword));
        }
        
        static boolean isDramaOrEntertainmentContent(String titleLower, String descLower) {
            return Arrays.stream(DRAMA_ENTERTAINMENT_KEYWORDS)
                    .anyMatch(keyword -> titleLower.contains(keyword.toLowerCase()) || descLower.contains(keyword.toLowerCase()));
        }
    }
    
    private static class ContentValidator {
        static boolean hasValidSpaceContent(String titleLower, String descLower) {
            int spaceKeywordCount = 0;
            List<String> foundKeywords = new ArrayList<>();
            
            // 정확한 키워드 매칭 (단어 경계 고려)
            for (String keyword : KeywordConstants.KOREAN_KEYWORDS) {
                if (containsExactKeyword(titleLower, keyword.toLowerCase()) || 
                    containsExactKeyword(descLower, keyword.toLowerCase())) {
                    spaceKeywordCount++;
                    foundKeywords.add(keyword);
                }
            }
            
            for (String keyword : KeywordConstants.ENGLISH_KEYWORDS) {
                if (containsExactKeyword(titleLower, keyword.toLowerCase()) || 
                    containsExactKeyword(descLower, keyword.toLowerCase())) {
                    spaceKeywordCount++;
                    foundKeywords.add(keyword);
                }
            }
            
            // 최소 키워드 개수 증가 (더 엄격하게)
            if (spaceKeywordCount < 3) {
                return false;
            }
            
            // "태양" 키워드 특별 처리 강화
            if (foundKeywords.contains("태양") || foundKeywords.contains("sun")) {
                if (titleLower.contains("태양의") || titleLower.contains("descendants") ||
                    titleLower.contains("사랑") || titleLower.contains("love") ||
                    titleLower.contains("드라마") || titleLower.contains("drama")) {
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
                "화성탐사", "mars exploration", "달탐사", "lunar exploration",
                "국제우주정거장", "international space station", "iss",
                "아르테미스", "artemis", "아폴로", "apollo"
            };
            
            boolean hasProfessionalKeyword = Arrays.stream(professionalKeywords)
                    .anyMatch(k -> containsExactKeyword(titleLower, k) || containsExactKeyword(descLower, k));
            
            if (hasProfessionalKeyword) {
                return true;
            }
            
            // 일반 키워드는 더 많이 필요
            return spaceKeywordCount >= 4;
        }
        
        // 정확한 키워드 매칭을 위한 헬퍼 메서드
        private static boolean containsExactKeyword(String text, String keyword) {
            if (keyword.length() <= 2) {
                // 짧은 키워드는 단어 경계로 체크
                return text.matches(".*\\b" + keyword + "\\b.*");
            } else {
                // 긴 키워드는 포함 여부만 체크
                return text.contains(keyword);
            }
        }
    }
}