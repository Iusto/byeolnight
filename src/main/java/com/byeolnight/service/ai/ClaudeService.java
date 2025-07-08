package com.byeolnight.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeService {

    @Value("${claude.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateDiscussionTopic() {
        if (apiKey.isEmpty()) {
            return generateFallbackTopic();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            String prompt = """
                우주와 관련된 흥미로운 토론 주제를 하나 생성해주세요.
                다음 형식으로 응답해주세요:
                제목: [30자 이내의 흥미로운 제목]
                설명: [200자 이내의 토론을 유도하는 설명]
                
                주제는 우주 탐사, 외계 생명체, 우주 기술, 천체 현상 등과 관련되어야 하며,
                사용자들이 다양한 의견을 나눌 수 있도록 논쟁적이거나 흥미로운 관점을 제시해야 합니다.
                """;

            Map<String, Object> requestBody = Map.of(
                "model", "claude-3-sonnet-20240229",
                "max_tokens", 300,
                "messages", new Object[]{
                    Map.of("role", "user", "content", prompt)
                }
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.anthropic.com/v1/messages",
                HttpMethod.POST,
                entity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Object content = ((Map<String, Object>) ((Object[]) responseBody.get("content"))[0]).get("text");
                return content.toString();
            }
        } catch (Exception e) {
            log.error("Claude API 호출 실패", e);
        }

        return generateFallbackTopic();
    }

    private String generateFallbackTopic() {
        String[] topics = {
            "제목: 화성 이주, 현실적으로 가능할까?\n설명: 스페이스X와 NASA의 화성 이주 계획이 구체화되고 있습니다. 기술적 한계, 비용, 윤리적 문제 등을 고려할 때 화성 이주는 언제쯤 현실이 될 수 있을까요? 여러분의 생각을 들려주세요.",
            "제목: 외계 생명체 발견 시 인류의 대응\n설명: 만약 지적 외계 생명체와 접촉하게 된다면 인류는 어떻게 대응해야 할까요? 과학적 교류, 문화적 충돌, 종교적 영향 등 다양한 관점에서 토론해보세요.",
            "제목: 우주 쓰레기 문제의 해결책\n설명: 지구 궤도상의 우주 쓰레기가 심각한 문제가 되고 있습니다. 케슬러 신드롬의 위험성과 함께 이를 해결할 수 있는 현실적인 방안에 대해 의견을 나눠보세요.",
            "제목: 우주 관광의 미래와 한계\n설명: 민간 우주 관광이 현실화되고 있지만 여전히 높은 비용과 안전성 문제가 있습니다. 우주 관광이 대중화될 수 있을지, 그 과정에서 어떤 변화가 필요한지 토론해보세요."
        };
        
        int randomIndex = (int) (Math.random() * topics.length);
        return topics[randomIndex];
    }
}