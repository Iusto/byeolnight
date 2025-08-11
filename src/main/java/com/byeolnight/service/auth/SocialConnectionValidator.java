package com.byeolnight.service.auth;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialConnectionValidator {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 매일 오전 10시 - 활성 소셜 사용자 연동 상태 검증
     */
    @Scheduled(cron = "0 0 10 * * *")
    @Transactional
    public void validateSocialConnections() {
        log.info("🔍 소셜 연동 상태 검증 시작");
        
        List<User> activeSocialUsers = userRepository.findByPasswordIsNullAndStatus(User.UserStatus.ACTIVE);
        int disconnectedCount = 0;
        
        for (User user : activeSocialUsers) {
            try {
                if (!isConnectionValid(user)) {
                    com.byeolnight.service.auth.SocialAccountCleanupService cleanupService = 
                        com.byeolnight.infrastructure.config.ApplicationContextProvider
                            .getBean(com.byeolnight.service.auth.SocialAccountCleanupService.class);
                    cleanupService.handleSocialDisconnection(user.getEmail(), user.getSocialProvider());
                    disconnectedCount++;
                }
            } catch (Exception e) {
                log.error("연동 상태 확인 실패: {}", user.getEmail(), e);
            }
        }
        
        log.info("🔍 소셜 연동 검증 완료: {}개 연동 해제 감지", disconnectedCount);
    }
    
    private boolean isConnectionValid(User user) {
        String provider = user.getSocialProvider();
        if (provider == null) return false;
        
        return switch (provider.toLowerCase()) {
            case "kakao" -> validateKakaoConnection(user);
            case "naver" -> validateNaverConnection(user);
            case "google" -> validateGoogleConnection(user);
            default -> true; // 알 수 없는 제공자는 유지
        };
    }
    
    private boolean validateKakaoConnection(User user) {
        // Kakao API로 사용자 정보 조회 시도
        // 실패하면 연동 해제된 것으로 판단
        return true; // 실제 구현 필요
    }
    
    private boolean validateNaverConnection(User user) {
        // Naver API로 사용자 정보 조회 시도
        return true; // 실제 구현 필요
    }
    
    private boolean validateGoogleConnection(User user) {
        // Google API로 사용자 정보 조회 시도
        return true; // 실제 구현 필요
    }
}