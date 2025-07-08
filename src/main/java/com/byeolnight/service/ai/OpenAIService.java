package com.byeolnight.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIService {

    @Value("${openai.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateDiscussionTopic() {
        log.info("OpenAI API 키 상태: {}", apiKey.isEmpty() ? "비어있음" : "설정됨");
        if (apiKey.isEmpty()) {
            log.warn("OpenAI API 키가 설정되지 않아 fallback 주제 사용");
            return generateFallbackTopic();
        }
        
        // TODO: OpenAI 크레딧 충전 후 아래 주석을 해제하고 이 return문을 삭제
        // 현재는 할당량 초과로 인해 fallback 주제만 사용
        log.info("할당량 문제로 인해 fallback 주제 사용");
        return generateFallbackTopic();

        /*
        // OpenAI 크레딧 충전 후 활성화할 코드:
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String prompt = """
                우주 커뮤니티 '별 헤는 밤'을 위한 토론 주제를 생성해주세요.
                
                요구사항:
                - 제목: 30자 이내의 흥미로운 제목
                - 내용: 200자 이내의 토론을 유도하는 설명
                - 주제: 우주 탐사, 외계 생명체, 천체 물리학, 우주 기술, 과학 철학 등
                - 톤: 지적이고 품격 있는 커뮤니티에 어울리는 진지한 톤
                
                다음 형식으로만 응답해주세요:
                제목: [제목]
                내용: [내용]
                
                예시:
                제목: 우주는 유한한 공간일까?
                내용: 우주의 크기는 과연 끝이 있는 유한한 공간일까요, 아니면 무한히 확장되는 구조일까요? 다양한 이론과 관점을 바탕으로 여러분의 생각을 공유해주세요.
                """;

            Map<String, Object> requestBody = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 300,
                "temperature", 0.8
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
            log.error("OpenAI API 호출 실패 - API 키: {}", apiKey.isEmpty() ? "없음" : "있음", e);
        }

        return generateFallbackTopic();
        */
    }

    private String generateFallbackTopic() {
        String[] topics = {
            "제목: 화성 이주, 현실적으로 가능할까?\n내용: 스페이스X와 NASA의 화성 이주 계획이 구체화되고 있습니다. 기술적 한계, 비용, 윤리적 문제 등을 고려할 때 화성 이주는 언제쯤 현실이 될 수 있을까요? 여러분의 생각을 들려주세요.",
            "제목: 외계 생명체 발견 시 인류의 대응\n내용: 만약 지적 외계 생명체와 접촉하게 된다면 인류는 어떻게 대응해야 할까요? 과학적 교류, 문화적 충돌, 종교적 영향 등 다양한 관점에서 토론해보세요.",
            "제목: 우주 쓰레기 문제의 해결책\n내용: 지구 궤도상의 우주 쓰레기가 심각한 문제가 되고 있습니다. 케슬러 신드롬의 위험성과 함께 이를 해결할 수 있는 현실적인 방안에 대해 의견을 나눠보세요.",
            "제목: 우주 관광의 미래와 한계\n내용: 민간 우주 관광이 현실화되고 있지만 여전히 높은 비용과 안전성 문제가 있습니다. 우주 관광이 대중화될 수 있을지, 그 과정에서 어떤 변화가 필요한지 토론해보세요.",
            "제목: 블랙홀의 정보 역설\n내용: 호킹 복사로 인해 블랙홀이 증발할 때 정보는 어떻게 될까요? 정보가 사라지는 것인지, 아니면 다른 형태로 보존되는 것인지에 대한 여러분의 견해를 나눠주세요."
        };
        
        int randomIndex = (int) (Math.random() * topics.length);
        return topics[randomIndex];
    }
}