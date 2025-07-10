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

    private final List<String> SPACE_KEYWORDS = Arrays.asList(
        "NASA space documentary",
        "astronomy science",
        "universe documentary",
        "black hole science",
        "galaxy formation",
        "mars exploration",
        "space station ISS",
        "solar system planets",
        "SpaceX launch",
        "Hubble telescope"
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
            Map<String, String> videoData = fetchRandomSpaceVideo();
            if (videoData != null) {
                createCinemaPost(admin, videoData.get("title"), videoData.get("content"));
            }
        } catch (Exception e) {
            log.error("수동 별빛 시네마 포스팅 실패", e);
            throw new RuntimeException("별빛 시네마 포스팅에 실패했습니다.");
        }
    }

    private void createCinemaPost(User user, String title, String content) {
        Post post = Post.builder()
            .title(title)
            .content(content)
            .category(Post.Category.STARLIGHT_CINEMA)
            .writer(user)
            .build();

        postRepository.save(post);
        log.info("별빛 시네마 게시글 생성 완료: {}", title);
    }

    private Map<String, String> fetchRandomSpaceVideo() {
        if (googleApiKey == null || googleApiKey.isEmpty()) {
            return createMockVideoData();
        }

        try {
            String keyword = SPACE_KEYWORDS.get(new Random().nextInt(SPACE_KEYWORDS.size()));
            String url = String.format(
                "https://www.googleapis.com/youtube/v3/search?part=snippet&q=%s&type=video&maxResults=10&order=date&publishedAfter=%s&key=%s",
                keyword, getOneYearAgo(), googleApiKey
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                
                if (items != null && !items.isEmpty()) {
                    Map<String, Object> video = items.get(new Random().nextInt(items.size()));
                    @SuppressWarnings("unchecked")
                    Map<String, Object> snippet = (Map<String, Object>) video.get("snippet");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> videoId = (Map<String, Object>) video.get("id");
                    
                    return formatVideoPost(
                        (String) snippet.get("title"),
                        (String) snippet.get("description"),
                        (String) videoId.get("videoId"),
                        (String) snippet.get("channelTitle")
                    );
                }
            }
        } catch (Exception e) {
            log.error("YouTube API 호출 실패", e);
        }

        return createMockVideoData();
    }

    private Map<String, String> createMockVideoData() {
        String[] mockTitles = {
            "우주의 신비: 블랙홀의 비밀",
            "은하수 너머의 세계",
            "화성 탐사의 최신 소식",
            "별의 탄생과 죽음",
            "우주 정거장에서의 하루"
        };
        
        String[] mockDescriptions = {
            "우주의 가장 신비로운 천체인 블랙홀에 대해 알아봅시다.",
            "우리 은하 너머에 존재하는 놀라운 우주의 모습을 탐험해보세요.",
            "화성 탐사 로버가 전해주는 최신 발견들을 소개합니다.",
            "별들이 어떻게 태어나고 생을 마감하는지 알아봅시다.",
            "국제우주정거장에서 우주비행사들의 일상을 엿보세요."
        };

        Random random = new Random();
        int index = random.nextInt(mockTitles.length);
        
        return formatVideoPost(
            mockTitles[index],
            mockDescriptions[index],
            "dQw4w9WgXcQ",
            "우주 채널"
        );
    }

    private Map<String, String> formatVideoPost(String title, String description, String videoId, String channelTitle) {
        String aiSummary = generateAISummary(title, description);
        String enhancedTitle = enhanceTitle(title);
        String cleanDescription = cleanDescription(description);
        
        String content = String.format("""
            # %s
            
            ## 🤖 요약
            %s
            
            ## 📺 영상 보기
            
            <iframe width="100%%" height="500" 
              src="https://www.youtube.com/embed/%s" 
              title="%s"
              frameborder="0" 
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
              allowfullscreen
              style="border-radius: 12px; box-shadow: 0 8px 32px rgba(139, 92, 246, 0.3);">
            </iframe>
            
            **[🎆 YouTube에서 시청하기 →](https://www.youtube.com/watch?v=%s)**
            
            ## 🎬 영상 정보
            - **제목**: %s
            - **채널**: %s
            
            ## 📄 영상 설명
            %s
            
            🛰️ **매일 밤, 별빛 시네마에서는 최신 우주 탐사 소식을 전합니다.**  
            💬 **여러분의 생각은 어떤가요? 댓글로 자유롭게 나눠주세요!**
            """, 
            enhancedTitle, aiSummary, videoId, title, videoId, title, channelTitle, cleanDescription);
            
        Map<String, String> result = new HashMap<>();
        result.put("title", enhancedTitle);
        result.put("content", content);
        return result;
    }
    
    private String getOneYearAgo() {
        return java.time.LocalDateTime.now().minusYears(1).format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        );
    }
    
    private String cleanDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "이 영상은 우주와 천문학의 흥미진진한 세계를 탐험합니다. 최신 과학 연구와 놀라운 발견들을 통해 우주의 신비를 함께 풀어나가보세요.";
        }
        
        // URL 제거 및 정리
        String cleaned = description.replaceAll("https?://[^\\s]+", "")
                                   .replaceAll("\\n+", " ")
                                   .trim();
        
        // 200자 제한
        if (cleaned.length() > 200) {
            cleaned = cleaned.substring(0, 200) + "...";
        }
        
        return cleaned.isEmpty() ? "이 영상은 우주와 천문학의 흥미진진한 세계를 탐험합니다." : cleaned;
    }
    
    private String enhanceTitle(String originalTitle) {
        // 영어 제목을 한국어로 번역
        String translatedTitle = translateTitle(originalTitle);
        
        // 제목을 더 매력적으로 만들기
        if (translatedTitle.toLowerCase().contains("nasa") || originalTitle.toLowerCase().contains("nasa")) {
            return "🚨 " + translatedTitle;
        } else if (translatedTitle.contains("화성") || originalTitle.toLowerCase().contains("mars")) {
            return "🔴 " + translatedTitle;
        } else if (translatedTitle.contains("우주") || originalTitle.toLowerCase().contains("space")) {
            return "🌌 " + translatedTitle;
        } else if (translatedTitle.contains("블랙홀") || originalTitle.toLowerCase().contains("black hole")) {
            return "⚫ " + translatedTitle;
        } else {
            return "✨ " + translatedTitle;
        }
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
                    "당신은 전문 번역가입니다. YouTube 영상 제목을 자연스럽고 매력적인 한국어로 번역해주세요. 과학적 용어는 정확하게 번역하고, 제목만 반환해주세요."),
                Map.of("role", "user", "content", 
                    "다음 영어 제목을 한국어로 번역해주세요: " + englishTitle)
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
        // 기본적인 단어 치환
        String translated = englishTitle
            .replaceAll("(?i)NASA", "NASA")
            .replaceAll("(?i)Mars", "화성")
            .replaceAll("(?i)Space", "우주")
            .replaceAll("(?i)Black Hole", "블랙홀")
            .replaceAll("(?i)Galaxy", "은하")
            .replaceAll("(?i)Planet", "행성")
            .replaceAll("(?i)Star", "별")
            .replaceAll("(?i)Universe", "우주")
            .replaceAll("(?i)Solar System", "태양계")
            .replaceAll("(?i)Astronomy", "천문학")
            .replaceAll("(?i)Documentary", "다큐멘터리")
            .replaceAll("(?i)Telescope", "망원경")
            .replaceAll("(?i)Satellite", "위성")
            .replaceAll("(?i)Rocket", "로켓")
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
            requestBody.put("max_tokens", 150);  // 토큰 수 제한
            requestBody.put("temperature", 0.5);  // 더 일관된 응답
            
            List<Map<String, String>> messages = Arrays.asList(
                Map.of("role", "system", "content", 
                    "당신은 우주와 천문학 전문가입니다. YouTube 영상의 제목과 설명을 바탕으로 " +
                    "한국어로 3-4줄의 흥미로운 요약을 작성해주세요. 이모지를 적절히 사용하고, " +
                    "과학적 정확성을 유지하면서도 일반인이 이해하기 쉽게 설명해주세요."),
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
        String[] summaryTemplates = {
            "%s에 대한 놀라운 사실들이 공개됩니다.\n최신 과학 연구 결과와 전문가들의 분석을 통해 우주의 신비를 파헤쳐봅니다.\n이 영상 하나로 당신의 우주관이 완전히 바뀔 것입니다.",
            "%s의 세계로 떠나는 특별한 여행이 시작됩니다.\n복잡한 과학 이론을 쉽고 명확하게 설명하여 누구나 이해할 수 있습니다.\n우주 탐사 역사상 가장 흥미진진한 순간들을 만나보세요.",
            "%s에 관한 최신 발견과 미래 전망을 다룹니다.\n NASA와 세계 각국의 우주기관이 밝혀낸 놀라운 진실들.\n과학적 호기심을 자극하는 고품질 콘텐츠입니다.",
            "%s를 둘러싼 미스터리가 마침내 해결됩니다.\n전문가들도 놀란 새로운 관점과 통찰력을 제공합니다.\n우주의 광대함 앞에서 느끼는 경이로움을 함께 나누세요."
        };
        
        String template = summaryTemplates[new Random().nextInt(summaryTemplates.length)];
        String topic = extractTopicFromTitle(title);
        return String.format(template, topic);
    }
    
    private String extractTopicFromTitle(String title) {
        String[] keywords = {"우주", "블랙홀", "은하", "별", "행성", "천문학", "과학", "탐험"};
        for (String keyword : keywords) {
            if (title.toLowerCase().contains(keyword.toLowerCase())) {
                return keyword;
            }
        }
        return "우주의 신비";
    }

    private void createCinemaPost(User user, String content) {
        String title = extractTitleFromContent(content);
        
        Post post = Post.builder()
            .title(title)
            .content(content)
            .category(Post.Category.STARLIGHT_CINEMA)
            .writer(user)
            .build();

        postRepository.save(post);
        log.info("별빛 시네마 게시글 생성 완료: {}", title);
    }

    private String extractTitleFromContent(String content) {
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.startsWith("# 🎬 ")) {
                return line.substring(5).trim();
            }
        }
        return "오늘의 우주 영상";
    }

    private User getSystemUser() {
        return userRepository.findByEmail("system@byeolnight.com")
            .orElseThrow(() -> new RuntimeException("시스템 사용자를 찾을 수 없습니다."));
    }
}