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
        "space documentary",
        "astronomy",
        "universe",
        "black hole",
        "galaxy",
        "nasa",
        "space exploration",
        "cosmos",
        "astrophysics",
        "solar system"
    );

    @Scheduled(cron = "0 0 20 * * ?") // 매일 오후 8시
    @Transactional
    public void createDailyCinemaPost() {
        try {
            log.info("별빛 시네마 자동 포스팅 시작");
            
            User systemUser = getSystemUser();
            String videoData = fetchRandomSpaceVideo();
            
            if (videoData != null) {
                createCinemaPost(systemUser, videoData);
                log.info("별빛 시네마 포스팅 완료");
            }
        } catch (Exception e) {
            log.error("별빛 시네마 자동 포스팅 실패", e);
        }
    }

    public void createCinemaPostManually(User admin) {
        try {
            String videoData = fetchRandomSpaceVideo();
            if (videoData != null) {
                createCinemaPost(admin, videoData);
            }
        } catch (Exception e) {
            log.error("수동 별빛 시네마 포스팅 실패", e);
            throw new RuntimeException("별빛 시네마 포스팅에 실패했습니다.");
        }
    }

    private String fetchRandomSpaceVideo() {
        if (googleApiKey == null || googleApiKey.isEmpty()) {
            return createMockVideoData();
        }

        try {
            String keyword = SPACE_KEYWORDS.get(new Random().nextInt(SPACE_KEYWORDS.size()));
            String url = String.format(
                "https://www.googleapis.com/youtube/v3/search?part=snippet&q=%s&type=video&maxResults=10&key=%s",
                keyword, googleApiKey
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

    private String createMockVideoData() {
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

    private String formatVideoPost(String title, String description, String videoId, String channelTitle) {
        String aiSummary = generateAISummary(title, description);
        
        return String.format("""
            # 🎬 %s
            
            **채널:** %s
            
            ## 🤖 AI 요약
            %s
            
            ---
            
            ## 📝 영상 설명
            %s
            
            ---
            
            ### 📺 영상 시청하기
            
            [![%s](https://img.youtube.com/vi/%s/maxresdefault.jpg)](https://www.youtube.com/watch?v=%s)
            
            **[YouTube에서 시청하기 →](https://www.youtube.com/watch?v=%s)**
            
            ---
            
            💫 **별빛 시네마**에서는 매일 우주와 천문학 관련 흥미로운 영상을 소개합니다!
            
            🌟 이 영상이 마음에 드셨다면 좋아요와 댓글로 여러분의 생각을 나눠주세요.
            """, 
            title, channelTitle, aiSummary, description, title, videoId, videoId, videoId);
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
            "🌌 이 영상은 %s에 대한 흥미진진한 탐험을 다룹니다. 최신 과학 연구와 놀라운 발견들을 통해 우주의 신비를 풀어나가는 여정을 함께해보세요. 🚀",
            "⭐ %s의 세계로 떠나는 특별한 여행! 복잡한 우주 과학을 쉽고 재미있게 설명하여 누구나 이해할 수 있도록 구성되었습니다. 🔭",
            "🪐 %s에 관한 최신 정보와 흥미로운 사실들을 담은 영상입니다. 우주의 광대함과 아름다움을 느낄 수 있는 시간이 될 것입니다. ✨",
            "🌟 %s를 주제로 한 교육적이면서도 재미있는 콘텐츠입니다. 과학적 호기심을 자극하는 내용으로 가득 차 있어요! 🛸"
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