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

    @Value("${app.security.external-api.ai.google-api-key}")
    private String googleApiKey;
    
    public GoogleVisionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        log.info("GoogleVisionService 초기화 완료");
    }
    
    private static final String VISION_API_URL = "https://vision.googleapis.com/v1/images:annotate";
    
    public boolean isImageSafe(byte[] imageBytes) {
        // API 키 검증
        if (googleApiKey == null || googleApiKey.trim().isEmpty()) {
            log.error("🚫 Google API 키가 설정되지 않았습니다. 이미지 검열을 수행할 수 없습니다.");
            return false; // API 키가 없으면 안전하지 않다고 판단
        }
        
        try {
            log.info("🔍 Google Vision API로 이미지 검열 시작 (크기: {}KB)", imageBytes.length / 1024);
            
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
            
            log.info("🌐 Google Vision API 호출: {}", VISION_API_URL);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("✅ Google Vision API 응답 수신 성공");
                return analyzeSafeSearchResult(response.getBody());
            } else {
                log.error("❌ Google Vision API 호출 실패 - 상태코드: {}", response.getStatusCode());
                return false;
            }
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.error("🚫 Google Vision API 권한 오류 (403): API 키가 잘못되었거나 Vision API가 활성화되지 않았습니다.");
                log.error("해결 방법: 1) Google Cloud Console에서 Vision API 활성화 2) API 키 확인");
            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.error("🚫 Google Vision API 요청 오류 (400): {}", e.getResponseBodyAsString());
            } else {
                log.error("🚫 Google Vision API HTTP 오류 ({}): {}", e.getStatusCode(), e.getMessage());
            }
            return false;
        } catch (Exception e) {
            log.error("🚫 이미지 검열 중 예상치 못한 오류 발생", e);
            return false;
        }
    }
    
    @SuppressWarnings("unchecked")
    private boolean analyzeSafeSearchResult(Map<String, Object> response) {
        try {
            var responses = (java.util.List<Map<String, Object>>) response.get("responses");
            if (responses == null || responses.isEmpty()) {
                log.warn("Google Vision API 응답이 비어있습니다.");
                return false;
            }
            
            var safeSearchAnnotation = (Map<String, String>) responses.get(0).get("safeSearchAnnotation");
            if (safeSearchAnnotation == null) {
                log.warn("SafeSearch 주석이 없습니다.");
                return false;
            }
            
            String adult = safeSearchAnnotation.getOrDefault("adult", "UNKNOWN");
            String violence = safeSearchAnnotation.getOrDefault("violence", "UNKNOWN");
            String racy = safeSearchAnnotation.getOrDefault("racy", "UNKNOWN");
            String spoof = safeSearchAnnotation.getOrDefault("spoof", "UNKNOWN");
            String medical = safeSearchAnnotation.getOrDefault("medical", "UNKNOWN");
            
            boolean isSafe = isLevelSafe(adult) && isLevelSafe(violence) && 
                           isLevelSafe(racy) && isLevelSafe(spoof) && isLevelSafe(medical);
            
            log.info("🔍 이미지 검열 상세 결과:");
            log.info("  - Adult: {} ({})", adult, isLevelSafe(adult) ? "✅" : "❌");
            log.info("  - Violence: {} ({})", violence, isLevelSafe(violence) ? "✅" : "❌");
            log.info("  - Racy: {} ({})", racy, isLevelSafe(racy) ? "✅" : "❌");
            log.info("  - Spoof: {} ({})", spoof, isLevelSafe(spoof) ? "✅" : "❌");
            log.info("  - Medical: {} ({})", medical, isLevelSafe(medical) ? "✅" : "❌");
            log.info("  - 최종 결과: {} {}", isSafe ? "안전" : "부적절", isSafe ? "✅" : "🚫");
            
            return isSafe;
            
        } catch (Exception e) {
            log.error("Safe Search 결과 분석 중 오류", e);
            return false;
        }
    }
    
    private boolean isLevelSafe(String level) {
        return "VERY_UNLIKELY".equals(level) || "UNLIKELY".equals(level);
    }
}