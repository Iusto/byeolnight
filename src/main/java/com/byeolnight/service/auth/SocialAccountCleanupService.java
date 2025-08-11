package com.byeolnight.service.auth;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 소셜 로그인 계정 정리 서비스
 * - 연결 해제된 소셜 계정 감지 및 정리
 * - 좀비 계정 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialAccountCleanupService {

    private final UserRepository userRepository;

    /**
     * 소셜 로그인 실패 시 계정 처리
     */
    @Transactional
    public void handleFailedSocialLogin(String email, String provider) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.isSocialUser() && user.getStatus() == User.UserStatus.ACTIVE) {
                // 연결해제 의심 - 계정 일시 정지
                user.changeStatus(User.UserStatus.SUSPENDED);
                log.warn("소셜 로그인 실패로 계정 일시 정지: {} (제공자: {})", email, provider);
            }
        });
    }

    /**
     * 매일 오전 10시 - 비활성 소셜 계정 정리
     */
    @Scheduled(cron = "0 0 10 * * *")
    @Transactional
    public void cleanupOrphanedSocialAccounts() {
        log.info("🧹 소셜 계정 정리 작업 시작");
        
        try {
            // 6개월 이상 미로그인 + 일시정지된 소셜 계정
            LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
            List<User> suspendedSocialUsers = userRepository.findSocialUsersForCleanup(sixMonthsAgo, User.UserStatus.SUSPENDED);
            
            int cleanedCount = 0;
            for (User user : suspendedSocialUsers) {
                if (user.isSocialUser() && user.getStatus() == User.UserStatus.SUSPENDED) {
                    user.withdraw("소셜 로그인 연결 해제 추정 - 자동 정리");
                    cleanedCount++;
                    log.info("비활성 소셜 계정 자동 탈퇴: {}", user.getEmail());
                }
            }
            
            log.info("🧹 소셜 계정 정리 완료: {}개 계정 처리", cleanedCount);
            
        } catch (Exception e) {
            log.error("소셜 계정 정리 작업 중 오류 발생", e);
        }
    }

    /**
     * 계정 복구 요청 처리
     */
    @Transactional
    public boolean requestAccountRecovery(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> user.isSocialUser() && user.getStatus() == User.UserStatus.SUSPENDED)
                .map(user -> {
                    user.changeStatus(User.UserStatus.ACTIVE);
                    log.info("소셜 계정 복구: {}", email);
                    return true;
                })
                .orElse(false);
    }
}