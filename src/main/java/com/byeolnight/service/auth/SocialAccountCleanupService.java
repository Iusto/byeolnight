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
     * 소셜 로그인 실패 시 연동 해제 감지 및 즉시 처리
     */
    @Transactional
    public void handleFailedSocialLogin(String email, String provider) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.isSocialUser() && user.getStatus() == User.UserStatus.ACTIVE) {
                handleSocialDisconnection(email, provider);
            }
        });
    }

    /**
     * 소셜 연동 해제 즉시 처리 (30일 복구 가능)
     */
    @Transactional
    public void handleSocialDisconnection(String email, String provider) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.isSocialUser() && user.getStatus() == User.UserStatus.ACTIVE) {
                user.withdraw("소셜 로그인 연결 해제 - 30일 복구 가능");
                log.info("소셜 연동 해제 즉시 탈퇴 처리: {} ({})", email, provider);
            }
        });
    }

    /**
     * 매일 오전 9시 - 30일 경과 계정 개인정보 마스킹 (복구 불가능)
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void maskPersonalInfoAfterThirtyDays() {
        log.info("🔒 30일 경과 계정 개인정보 마스킹 작업 시작");
        
        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            List<User> expiredUsers = userRepository.findByStatusAndWithdrawnAtBefore(
                User.UserStatus.WITHDRAWN, thirtyDaysAgo);
            
            int maskedCount = 0;
            for (User user : expiredUsers) {
                // 이미 마스킹된 계정은 건너뛰기
                if (!user.getEmail().startsWith("deleted_")) {
                    user.completelyRemovePersonalInfo();
                    maskedCount++;
                    log.info("개인정보 마스킹 완료: {}", user.getEmail());
                }
            }
            
            log.info("🔒 개인정보 마스킹 완료: {}개 계정 처리", maskedCount);
            
        } catch (Exception e) {
            log.error("개인정보 마스킹 작업 중 오류 발생", e);
        }
    }

    /**
     * 매일 오전 10시 - 5년 경과 소셜 계정 완전 삭제
     */
    @Scheduled(cron = "0 0 10 * * *")
    @Transactional
    public void cleanupWithdrawnSocialAccounts() {
        log.info("🧹 5년 경과 소셜 계정 완전 삭제 작업 시작");
        
        try {
            LocalDateTime fiveYearsAgo = LocalDateTime.now().minusYears(5);
            List<User> withdrawnUsers = userRepository.findByStatusAndWithdrawnAtBefore(
                User.UserStatus.WITHDRAWN, fiveYearsAgo);
            
            int deletedCount = 0;
            for (User user : withdrawnUsers) {
                if (user.isSocialUser()) {
                    userRepository.delete(user);
                    deletedCount++;
                    log.info("소셜 계정 완전 삭제: {}", user.getEmail());
                }
            }
            
            log.info("🧹 소셜 계정 완전 삭제 완료: {}개 계정 처리", deletedCount);
            
        } catch (Exception e) {
            log.error("소셜 계정 완전 삭제 작업 중 오류 발생", e);
        }
    }

    /**
     * 30일 내 소셜 계정 복구
     */
    @Transactional
    public boolean recoverWithdrawnAccount(String email) {
        // 탈퇴된 이메일 형태에서 원본 이메일 추출
        String originalEmail = email;
        if (email.startsWith("withdrawn_") && email.endsWith("@byeolnight.local")) {
            // withdrawn_123@byeolnight.local 형태에서 ID 추출하여 원본 계정 찾기
            try {
                String idStr = email.substring(10, email.indexOf("@"));
                Long userId = Long.parseLong(idStr);
                return userRepository.findById(userId)
                        .filter(user -> user.getStatus() == User.UserStatus.WITHDRAWN &&
                                user.getWithdrawnAt() != null &&
                                user.getWithdrawnAt().isAfter(LocalDateTime.now().minusDays(30)))
                        .map(user -> {
                            user.changeStatus(User.UserStatus.ACTIVE);
                            user.clearWithdrawalInfo();
                            // 원본 이메일로 복원하지 않고 새로운 소셜 로그인으로 처리
                            log.info("소셜 계정 복구 성공: ID={}", userId);
                            return true;
                        })
                        .orElse(false);
            } catch (NumberFormatException e) {
                log.warn("탈퇴 계정 ID 파싱 실패: {}", email);
                return false;
            }
        }
        
        return userRepository.findByEmail(originalEmail)
                .filter(user -> user.getStatus() == User.UserStatus.WITHDRAWN &&
                        user.getWithdrawnAt() != null &&
                        user.getWithdrawnAt().isAfter(LocalDateTime.now().minusDays(30)))
                .map(user -> {
                    user.changeStatus(User.UserStatus.ACTIVE);
                    user.clearWithdrawalInfo();
                    log.info("소셜 계정 복구 성공: {}", originalEmail);
                    return true;
                })
                .orElse(false);
    }
}