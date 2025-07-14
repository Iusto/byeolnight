package com.byeolnight.service.cinema;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.post.PostRepository;
import com.byeolnight.domain.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CinemaService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.api.key:}")
    private String googleApiKey;
    
    @Value("${openai.api.key:}")
    private String openaiApiKey;

    private final List<String> KOREAN_SPACE_KEYWORDS = Arrays.asList(
        "우주 다큐멘터리",
        "NASA 한국어",
        "블랙홀 과학",
        "화성 탐사",
        "스페이스X 발사"
    );
    
    private final List<String> ENGLISH_SPACE_KEYWORDS = Arrays.asList(
        "NASA space documentary",
        "universe documentary",
        "black hole science",
        "mars exploration",
        "SpaceX launch"
    );

    @Scheduled(cron = "0 0 20 * * ?") // 매일 오후 8시
    @Transactional
    public void createDailyCinemaPost() {
        try {
            log.info("별빛 시네마 자동 포스팅 시작");
            
            User systemUser = getSystemUser();
            Map<String, String> videoData = fetchRandomSpaceVideo();
            
            if (videoData != null) {
                createCinemaPost(systemUser, videoData.get("title"), videoData.get("content"));
                log.info("별빛 시네마 포스팅 완료");
            }
        } catch (Exception e) {
            log.error("별빛 시네마 자동 포스팅 실패", e);
        }
    }

    public void createCinemaPostManually(User admin) {
        try {
            log.info("수동 별빛 시네마 포스팅 시작 - 관리자: {}", admin.getNickname());
            
            Map<String, String> videoData = fetchRandomSpaceVideo();
            
            if (videoData != null) {
                createCinemaPost(admin, videoData.get("title"), videoData.get("content"));
                log.info("수동 별빛 시네마 포스팅 성공");
            } else {
                throw new RuntimeException("비디오 데이터를 가져오는데 실패했습니다.");
            }
        } catch (Exception e) {
            log.error("수동 별빛 시네마 포스팅 실패", e);
            throw new RuntimeException("별빛 시네마 포스팅에 실패했습니다: " + e.getMessage(), e);
        }
    }

    private void createCinemaPost(User user, String title, String content) {
        try {
            if (title == null || title.trim().isEmpty()) {
                throw new IllegalArgumentException("제목이 비어있습니다.");
            }
            if (content == null || content.trim().isEmpty()) {
                throw new IllegalArgumentException("내용이 비어있습니다.");
            }
            
            Post post = Post.builder()
                .title(title)
                .content(content)
                .category(Post.Category.STARLIGHT_CINEMA)
                .writer(user)
                .build();

            Post savedPost = postRepository.save(post);
            log.info("별빛 시네마 게시글 생성 완료 - ID: {}, 제목: {}", savedPost.getId(), title);
        } catch (Exception e) {
            log.error("게시글 생성 실패 - 제목: {}, 에러: {}", title, e.getMessage(), e);
            throw e;
        }
    }

    private Map<String, String> fetchRandomSpaceVideo() {
        if (googleApiKey == null || googleApiKey.isEmpty()) {
            return createMockVideoData();
        }

        try {
            Map<String, String> koreanVideo = fetchVideoByLanguage(KOREAN_SPACE_KEYWORDS, "ko");
            if (koreanVideo != null) {
                return koreanVideo;
            }
            
            Map<String, String> englishVideo = fetchVideoByLanguage(ENGLISH_SPACE_KEYWORDS, "en");
            if (englishVideo != null) {
                return englishVideo;
            }
            
        } catch (Exception e) {
            log.error("YouTube API 호출 실패", e);
        }

        return createMockVideoData();
    }
    
    private Map<String, String> fetchVideoByLanguage(List<String> keywords, String language) {
        try {
            String keyword = keywords.get(new Random().nextInt(keywords.size()));
            
            String url = String.format(
                "https://www.googleapis.com/youtube/v3/search?part=snippet&q=%s&type=video&maxResults=20&order=relevance&publishedAfter=%s&videoDuration=medium&videoDefinition=high&key=%s",
                keyword, getTwoYearsAgo(), googleApiKey
            );
            
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
                        LocalDate publishDate = parsePublishedDate(publishedAt);
                        
                        return formatVideoPost(
                            (String) snippet.get("title"),
                            (String) snippet.get("description"),
                            (String) videoId.get("videoId"),
                            (String) snippet.get("channelTitle"),
                            publishDate
                        );
                    }
                }
            }
        } catch (Exception e) {
            log.error("{} YouTube 영상 검색 실패", language, e);
        }
        return null;
    }
    
    private boolean isQualityVideo(Map<String, Object> video) {
        @SuppressWarnings("unchecked")
        Map<String, Object> snippet = (Map<String, Object>) video.get("snippet");
        String title = (String) snippet.get("title");
        String channelTitle = (String) snippet.get("channelTitle");
        String description = (String) snippet.get("description");
        
        if (title.toLowerCase().contains("shorts") || 
            title.toLowerCase().contains("#shorts") ||
            title.length() < 10 ||
            (description != null && description.length() < 50)) {
            return false;
        }
        
        String[] qualityChannels = {"NASA", "SpaceX", "ESA", "National Geographic", "Discovery", "Science Channel"};
        for (String channel : qualityChannels) {
            if (channelTitle.toLowerCase().contains(channel.toLowerCase())) {
                return true;
            }
        }
        
        String[] professionalTerms = {"documentary", "science", "research", "mission", "exploration"};
        for (String term : professionalTerms) {
            if (title.toLowerCase().contains(term)) {
                return true;
            }
        }
        
        return false;
    }
    
    private String getTwoYearsAgo() {
        return java.time.LocalDateTime.now().minusYears(2).format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        );
    }

    private Map<String, String> createMockVideoData() {
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
        
        return formatVideoPost(
            mockTitles[index],
            mockDescriptions[index],
            "dQw4w9WgXcQ",
            "우주 채널",
            LocalDate.now()
        );
    }

    private Map<String, String> formatVideoPost(String title, String description, String videoId, String channelTitle, LocalDate publishDate) {
        String aiSummary = generateAISummary(title, description);
        String enhancedTitle = enhanceTitle(title);
        String cleanDescription = cleanDescription(description);
        
        String content = buildVideoPostContent(enhancedTitle, aiSummary, videoId, channelTitle, cleanDescription, publishDate);
            
        Map<String, String> result = new HashMap<>();
        result.put("title", enhancedTitle);
        result.put("content", content);
        return result;
    }
    
    private String buildVideoPostContent(String enhancedTitle, String aiSummary, String videoId, 
                                       String channelTitle, String cleanDescription, LocalDate publishDate) {
        String formattedDate = publishDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));
        
        String iframeHtml = String.format("""
            <div class="video-container" style="position: relative; width: 100%%; padding-bottom: 56.25%%; height: 0; margin: 20px 0; border-radius: 16px; overflow: hidden; box-shadow: 0 12px 40px rgba(139, 92, 246, 0.4);">
              <iframe src="https://www.youtube.com/embed/%s?enablejsapi=1&rel=0&showinfo=0&modestbranding=1&autoplay=0" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen style="position: absolute; top: 0; left: 0; width: 100%%; height: 100%%; border: none;"></iframe>
            </div>
            <p>⚠️ 영상이 보이지 않나요? <a href="https://www.youtube.com/watch?v=%s" target="_blank">YouTube에서 보기</a></p>
            """, videoId, videoId);
        
        return """
            🎬 %s
            
            📌 요약  
            %s
            
            ▶️ 영상 보기  
            
            %s
            
            📺 채널명: %s  
            📅 발행일: %s  
            
            📝 설명  
            %s  
            
            🔗 [YouTube 바로가기](https://www.youtube.com/watch?v=%s)  
            
            💬 자유롭게 의견을 나눠주세요!
            """.formatted(enhancedTitle, aiSummary, iframeHtml, channelTitle, formattedDate, cleanDescription, videoId);
    }
    
    private String cleanDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "이 영상은 우주와 천문학의 흥미진진한 세계를 탐험합니다.";
        }
        
        String cleaned = description.replaceAll("https?://[^\\s]+", "")
                                   .replaceAll("\\n+", " ")
                                   .trim();
        
        if (cleaned.length() > 200) {
            cleaned = cleaned.substring(0, 200) + "...";
        }
        
        return cleaned.isEmpty() ? "이 영상은 우주와 천문학의 흥미진진한 세계를 탐험합니다." : cleaned;
    }
    
    private String enhanceTitle(String originalTitle) {
        String translatedTitle = translateTitle(originalTitle);
        return "오늘의 우주 영상: " + translatedTitle;
    }
    
    private String translateTitle(String englishTitle) {
        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            return translateTitleBasic(englishTitle);
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("max_tokens", 100);
            requestBody.put("temperature", 0.3);
            
            List<Map<String, String>> messages = Arrays.asList(
                Map.of("role", "system", "content", 
                    "YouTube 영상 제목을 자연스러운 한국어로 번역해주세요. 제목만 반환해주세요."),
                Map.of("role", "user", "content", 
                    "다음 제목을 한국어로 번역해주세요: " + englishTitle)
            );
            
            requestBody.put("messages", messages);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                "https://api.openai.com/v1/chat/completions", entity, Map.class);
            
            if (response != null && response.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String translated = (String) message.get("content");
                    return translated.trim().replaceAll("^\"?", "").replaceAll("\"?$", "");
                }
            }
        } catch (Exception e) {
            log.error("OpenAI 번역 API 호출 실패", e);
        }
        
        return translateTitleBasic(englishTitle);
    }
    
    private String translateTitleBasic(String englishTitle) {
        String translated = englishTitle
            .replaceAll("(?i)NASA", "NASA")
            .replaceAll("(?i)Mars", "화성")
            .replaceAll("(?i)Space", "우주")
            .replaceAll("(?i)Black Hole", "블랙홀")
            .replaceAll("(?i)Galaxy", "은하")
            .replaceAll("(?i)SpaceX", "스페이스X");
            
        return translated.length() > 50 ? translated.substring(0, 50) + "..." : translated;
    }
    
    private String generateAISummary(String title, String description) {
        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            return generateMockSummary(title);
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("max_tokens", 150);
            requestBody.put("temperature", 0.5);
            
            List<Map<String, String>> messages = Arrays.asList(
                Map.of("role", "system", "content", 
                    "YouTube 영상의 제목과 설명을 바탕으로 한국어로 3-4줄의 간결한 요약을 작성해주세요."),
                Map.of("role", "user", "content", 
                    String.format("제목: %s\n\n설명: %s", title, description))
            );
            
            requestBody.put("messages", messages);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                "https://api.openai.com/v1/chat/completions", entity, Map.class);
            
            if (response != null && response.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            log.error("OpenAI API 호출 실패", e);
        }
        
        return generateMockSummary(title);
    }
    
    private String generateMockSummary(String title) {
        return "이 영상은 우주와 천문학의 흥미진진한 세계를 탐험합니다. 최신 과학 연구와 놀라운 발견들을 통해 우주의 신비를 함께 풀어나가보세요.";
    }
    
    private LocalDate parsePublishedDate(String publishedAt) {
        try {
            return LocalDate.parse(publishedAt.substring(0, 10));
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private User getSystemUser() {
        return userRepository.findByEmail("system@byeolnight.com")
            .orElseThrow(() -> new RuntimeException("시스템 사용자를 찾을 수 없습니다."));
    }

    /**
     * 시네마 시스템 상태 조회
     */
    public Object getCinemaStatus() {
        Post latestCinemaPost = postRepository.findTopByCategoryOrderByCreatedAtDesc(Post.Category.STARLIGHT_CINEMA)
                .orElse(null);
        long totalCinemaPosts = postRepository.countByCategoryAndIsDeletedFalse(Post.Category.STARLIGHT_CINEMA);
        
        Map<String, Object> result = new HashMap<>();
        result.put("latestPostExists", latestCinemaPost != null);
        result.put("latestPostTitle", latestCinemaPost != null ? latestCinemaPost.getTitle() : null);
        result.put("totalCinemaPosts", totalCinemaPosts);
        result.put("lastUpdated", java.time.LocalDateTime.now());
        
        return result;
    }
}