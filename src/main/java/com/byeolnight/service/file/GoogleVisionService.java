package com.byeolnight.service.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
public class GoogleVisionService {
    
    private RestTemplate restTemplate;
    
    @Value("${google.api.key:}")
    private String googleApiKey;
    
    public GoogleVisionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        log.info("GoogleVisionService 초기화 완료");
    }
    
    private static final String VISION_API_URL = "https://vision.googleapis.com/v1/images:annotate";
    
    public boolean isImageSafe(byte[] imageBytes) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            
            Map<String, Object> request = Map.of(
                "requests", new Object[]{
                    Map.of(
                        "image", Map.of("content", base64Image),
                        "features", new Object[]{
                            Map.of("type", "SAFE_SEARCH_DETECTION", "maxResults", 1)
                        }
                    )
                }
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String url = VISION_API_URL + "?key=" + googleApiKey;
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return analyzeSafeSearchResult(response.getBody());
            }
            
            log.warn("Google Vision API 호출 실패");
            return true; // 기본적으로 안전하다고 가정
            
        } catch (Exception e) {
            log.error("이미지 검열 중 오류 발생", e);
            return true; // 오류 시 안전하다고 가정
        }
    }
    
    @SuppressWarnings("unchecked")
    private boolean analyzeSafeSearchResult(Map<String, Object> response) {
        try {
            var responses = (java.util.List<Map<String, Object>>) response.get("responses");
            if (responses == null || responses.isEmpty()) {
                return true;
            }
            
            var safeSearchAnnotation = (Map<String, String>) responses.get(0).get("safeSearchAnnotation");
            if (safeSearchAnnotation == null) {
                return true;
            }
            
            // VERY_UNLIKELY, UNLIKELY, POSSIBLE, LIKELY, VERY_LIKELY
            String adult = safeSearchAnnotation.getOrDefault("adult", "VERY_UNLIKELY");
            String violence = safeSearchAnnotation.getOrDefault("violence", "VERY_UNLIKELY");
            String racy = safeSearchAnnotation.getOrDefault("racy", "VERY_UNLIKELY");
            
            boolean isSafe = !"LIKELY".equals(adult) && !"VERY_LIKELY".equals(adult) &&
                           !"LIKELY".equals(violence) && !"VERY_LIKELY".equals(violence) &&
                           !"LIKELY".equals(racy) && !"VERY_LIKELY".equals(racy);
            
            log.info("이미지 검열 결과 - Adult: {}, Violence: {}, Racy: {}, Safe: {}", 
                    adult, violence, racy, isSafe);
            
            return isSafe;
            
        } catch (Exception e) {
            log.error("Safe Search 결과 분석 중 오류", e);
            return true;
        }
    }
}