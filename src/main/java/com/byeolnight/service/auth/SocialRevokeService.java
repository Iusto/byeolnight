package com.byeolnight.service.auth;

import com.byeolnight.entity.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialRevokeService {

    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${spring.security.oauth2.client.registration.kakao.client-id:}")
    private String kakaoClientId;
    
    @Value("${spring.security.oauth2.client.registration.naver.client-id:}")
    private String naverClientId;
    
    @Value("${spring.security.oauth2.client.registration.naver.client-secret:}")
    private String naverClientSecret;

    public void revokeKakaoConnection(User user) {
        try {
            String url = "https://kapi.kakao.com/v1/user/unlink";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "KakaoAK " + kakaoClientId);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Kakao 연동 해제 성공: {}", user.getEmail());
            } else {
                log.warn("Kakao 연동 해제 실패: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Kakao 연동 해제 중 오류: {}", e.getMessage());
        }
    }
    
    public void revokeNaverConnection(User user) {
        try {
            String url = "https://nid.naver.com/oauth2.0/token?grant_type=delete&client_id=" 
                + naverClientId + "&client_secret=" + naverClientSecret;
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.DELETE, null, String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Naver 연동 해제 성공: {}", user.getEmail());
            } else {
                log.warn("Naver 연동 해제 실패: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Naver 연동 해제 중 오류: {}", e.getMessage());
        }
    }
    
    public void revokeGoogleConnection(User user) {
        // Google은 사용자가 직접 Google 계정에서 앱 권한을 해제해야 함
        // 또는 저장된 refresh_token을 사용하여 revoke 가능
        log.info("Google 연동 해제 처리 (사용자 직접 해제 필요): {}", user.getEmail());
    }
}