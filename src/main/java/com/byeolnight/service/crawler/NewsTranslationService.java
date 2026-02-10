package com.byeolnight.service.crawler;

import com.byeolnight.dto.external.openai.OpenAiChatRequest;
import com.byeolnight.dto.external.openai.OpenAiChatResponse;
import com.byeolnight.dto.external.openai.OpenAiMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
public class NewsTranslationService {
    
    @Value("${app.security.external-api.ai.openai-api-key:}")
    private String openaiApiKey;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public String translateTitle(String englishTitle) {
        if (!isEnglishTitle(englishTitle) || openaiApiKey.isEmpty()) {
            return englishTitle;
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);
            
            String prompt = String.format(
                "다음 영어 뉴스 제목을 자연스러운 한국어로 번역해주세요: \"%s\"\n" +
                "우주/과학 전문 용어는 정확하게 번역하고, 번역문만 반환하세요.", 
                englishTitle
            );
            
            OpenAiChatRequest requestBody = OpenAiChatRequest.builder()
                .model("gpt-4o-mini")
                .messages(List.of(OpenAiMessage.user(prompt)))
                .maxTokens(100)
                .temperature(0.3)
                .build();

            HttpEntity<OpenAiChatRequest> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<OpenAiChatResponse> response = restTemplate.exchange(
                "https://api.openai.com/v1/chat/completions",
                HttpMethod.POST, entity, OpenAiChatResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String content = response.getBody().getFirstContent();
                if (content != null) {
                    return content.trim();
                }
            }
        } catch (Exception e) {
            log.warn("번역 실패: {} - {}", englishTitle, e.getMessage());
        }
        
        return "[해외뉴스] " + englishTitle;
    }
    
    public String generateAIAnalysis(String title, String description) {
        if (openaiApiKey.isEmpty()) {
            return "현재 이용 가능한 정보를 바탕으로 한 분석입니다. 더 자세한 내용은 원문 링크를 통해 확인하세요.";
        }
        
        try {
            String content = title + "\n" + (description != null ? description : "");
            String prompt = String.format(
                "다음 우주 뉴스를 분석하여 핵심 내용을 3-4개 포인트로 정리해주세요:\n\"%s\"\n" +
                "과학적 의미와 중요성을 일반인이 이해하기 쉽게 250자 내외로 작성하세요.",
                content
            );
            
            OpenAiChatRequest requestBody = OpenAiChatRequest.builder()
                .model("gpt-4o-mini")
                .messages(List.of(OpenAiMessage.user(prompt)))
                .maxTokens(300)
                .temperature(0.4)
                .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            HttpEntity<OpenAiChatRequest> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<OpenAiChatResponse> response = restTemplate.exchange(
                "https://api.openai.com/v1/chat/completions",
                HttpMethod.POST, entity, OpenAiChatResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String result = response.getBody().getFirstContent();
                if (result != null) {
                    return result.trim();
                }
            }
        } catch (Exception e) {
            log.warn("AI 분석 생성 실패: {} - {}", title, e.getMessage());
        }
        
        return "현재 이용 가능한 정보를 바탕으로 한 분석입니다. 더 자세한 내용은 원문 링크를 통해 확인하세요.";
    }
    
    private boolean isEnglishTitle(String title) {
        if (title == null) return false;
        
        int englishCount = 0;
        int koreanCount = 0;
        
        for (char c : title.toCharArray()) {
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                englishCount++;
            } else if (c >= '가' && c <= '힣') {
                koreanCount++;
            }
        }
        
        return englishCount > koreanCount;
    }
}