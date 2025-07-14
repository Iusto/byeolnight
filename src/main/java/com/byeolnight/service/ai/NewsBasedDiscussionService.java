package com.byeolnight.service.ai;

import com.byeolnight.domain.entity.News;
import com.byeolnight.domain.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsBasedDiscussionService {
    
    private final NewsRepository newsRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${openai.api.key:}")
    private String apiKey;
    
    public String generateNewsBasedDiscussion() {
        // 최근 3일 내 뉴스 중 토론 주제로 사용되지 않은 뉴스 조회
        Optional<News> newsOpt = getUnusedRecentNews();
        
        if (newsOpt.isEmpty()) {
            log.warn("토론 주제로 사용할 뉴스가 없어 fallback 주제 사용");
            return generateFallbackTopic();
        }
        
        News news = newsOpt.get();
        
        if (apiKey.isEmpty()) {
            log.warn("OpenAI API 키가 설정되지 않아 뉴스 기반 fallback 주제 사용");
            return generateNewsBasedFallback(news);
        }
        
        // GPT-3.5로 뉴스 기반 토론 주제 생성
        String discussionTopic = generateDiscussionWithGPT(news);
        
        // 토론 주제 생성 성공 시 뉴스를 사용됨으로 표시
        if (discussionTopic != null && !discussionTopic.contains("fallback")) {
            markNewsAsUsedForDiscussion(news);
        }
        
        return discussionTopic != null ? discussionTopic : generateNewsBasedFallback(news);
    }
    
    private Optional<News> getUnusedRecentNews() {
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        List<News> recentNews = newsRepository.findTop10ByCreatedAtAfterAndUsedForDiscussionFalseOrderByCreatedAtDesc(threeDaysAgo);
        
        return recentNews.isEmpty() ? Optional.empty() : Optional.of(recentNews.get(0));
    }
    
    private String generateDiscussionWithGPT(News news) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            String prompt = String.format("""
                다음 우주 뉴스를 바탕으로 토론 주제를 생성해주세요:
                
                뉴스 제목: %s
                뉴스 내용: %s
                
                요구사항:
                - 제목: 30자 이내의 흥미로운 토론 제목
                - 내용: 200자 이내의 토론을 유도하는 설명
                - 반드시 제공된 뉴스 내용과 직접적으로 연관된 토론 주제를 생성할 것
                - 뉴스에서 언급된 구체적 사실이나 연구 결과를 토론 내용에 포함할 것
                - 추상적이거나 일반적인 주제가 아닌, 해당 뉴스만의 고유한 쟁점을 다룰 것
                - 톤: 지적이고 품격 있는 커뮤니티에 어울리는 진지한 톤
                
                다음 형식으로만 응답해주세요:
                제목: [제목]
                내용: [내용]
                """, news.getTitle(), news.getDescription());
            
            Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 300,
                "temperature", 0.7
            );
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.openai.com/v1/chat/completions",
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            log.error("GPT API 호출 실패", e);
        }
        
        return null;
    }
    
    private String generateNewsBasedFallback(News news) {
        String title = news.getTitle();
        String description = news.getDescription();
        
        // 뉴스 제목을 기반으로 토론 주제 생성
        String discussionTitle = title.length() > 25 ? title.substring(0, 25) + "..." : title;
        
        return String.format("""
            제목: %s에 대한 여러분의 생각은?
            내용: 최근 발표된 '%s' 뉴스에 대해 어떻게 생각하시나요? %s 이 소식이 우주 과학과 인류의 미래에 미칠 영향에 대해 자유롭게 의견을 나눠주세요.
            """, discussionTitle, title, description != null ? description.substring(0, Math.min(description.length(), 100)) + "..." : "");
    }
    
    private void markNewsAsUsedForDiscussion(News news) {
        news.setUsedForDiscussion(true);
        newsRepository.save(news);
        log.info("뉴스 토론 사용 표시: {}", news.getTitle());
    }
    
    private String generateFallbackTopic() {
        String[] topics = {
            "제목: 화성 이주, 현실적으로 가능할까?\n내용: 스페이스X와 NASA의 화성 이주 계획이 구체화되고 있습니다. 기술적 한계, 비용, 윤리적 문제 등을 고려할 때 화성 이주는 언제쯤 현실이 될 수 있을까요?",
            "제목: 외계 생명체 발견 시 인류의 대응\n내용: 만약 지적 외계 생명체와 접촉하게 된다면 인류는 어떻게 대응해야 할까요? 과학적 교류, 문화적 충돌, 종교적 영향 등 다양한 관점에서 토론해보세요.",
            "제목: 우주 쓰레기 문제의 해결책\n내용: 지구 궤도상의 우주 쓰레기가 심각한 문제가 되고 있습니다. 케슬러 신드롬의 위험성과 함께 이를 해결할 수 있는 현실적인 방안에 대해 의견을 나눠보세요."
        };
        
        int randomIndex = (int) (Math.random() * topics.length);
        return topics[randomIndex];
    }
}