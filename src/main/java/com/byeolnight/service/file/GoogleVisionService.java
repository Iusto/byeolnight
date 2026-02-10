package com.byeolnight.service.file;

import com.byeolnight.dto.external.vision.VisionRequest;
import com.byeolnight.dto.external.vision.VisionResponse;
import com.byeolnight.dto.external.vision.VisionResponse.SafeSearchAnnotation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

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

            VisionRequest request = VisionRequest.safeSearchDetection(base64Image);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = VISION_API_URL + "?key=" + googleApiKey;
            HttpEntity<VisionRequest> entity = new HttpEntity<>(request, headers);

            log.info("🌐 Google Vision API 호출: {}", VISION_API_URL);
            ResponseEntity<VisionResponse> response = restTemplate.postForEntity(url, entity, VisionResponse.class);

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
    
    private boolean analyzeSafeSearchResult(VisionResponse response) {
        try {
            SafeSearchAnnotation annotation = response.getFirstSafeSearchAnnotation();
            if (annotation == null) {
                log.warn("SafeSearch 주석이 없습니다.");
                return false;
            }

            String adult = annotation.getAdultOrDefault();
            String violence = annotation.getViolenceOrDefault();
            String racy = annotation.getRacyOrDefault();
            String spoof = annotation.getSpoofOrDefault();
            String medical = annotation.getMedicalOrDefault();

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
        // 우주 관련 이미지의 특성을 고려하여 POSSIBLE까지 허용
        // LIKELY나 VERY_LIKELY만 차단
        return !"LIKELY".equals(level) && !"VERY_LIKELY".equals(level);
    }
}