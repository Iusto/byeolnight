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
     * 복구 가능한 계정이 있는지 확인 (복구하지 않음)
     */
    @Transactional(readOnly = true)
    public boolean hasRecoverableAccount(String email) {
        // 원본 이메일로 탈퇴된 계정 찾기
        return userRepository.findByEmail(email)
                .filter(user -> user.getStatus() == User.UserStatus.WITHDRAWN &&
                        user.getWithdrawnAt() != null &&
                        user.getWithdrawnAt().isAfter(LocalDateTime.now().minusDays(30)) &&
                        user.isSocialUser())
                .isPresent();
    }

    /**
     * 30일 내 소셜 계정 복구
     */
    @Transactional
    public boolean recoverWithdrawnAccount(String email) {
        // 원본 이메일로 탈퇴된 계정 찾기
        return userRepository.findByEmail(email)
                .filter(user -> user.getStatus() == User.UserStatus.WITHDRAWN &&
                        user.getWithdrawnAt() != null &&
                        user.getWithdrawnAt().isAfter(LocalDateTime.now().minusDays(30)))
                .map(user -> {
                    // 계정 상태를 활성화로 변경
                    user.changeStatus(User.UserStatus.ACTIVE);
                    user.clearWithdrawalInfo();
                    
                    // 원본 이메일과 닉네임으로 복원
                    String baseNickname = email.split("@")[0];
                    String uniqueNickname = generateUniqueNickname(baseNickname);
                    user.updateNickname(uniqueNickname, LocalDateTime.now());
                    
                    log.info("소셜 계정 복구 성공: {} -> 닉네임: {}", email, uniqueNickname);
                    return true;
                })
                .orElse(false);
    }
    
    /**
     * 고유한 닉네임 생성
     */
    private String generateUniqueNickname(String baseNickname) {
        String normalizedNickname = normalizeNickname(baseNickname);
        
        if (!userRepository.existsByNickname(normalizedNickname)) {
            return normalizedNickname;
        }
        
        // 중복된 경우 숫자 접미사 추가
        for (int i = 1; i <= 999; i++) {
            String candidateNickname = normalizedNickname + i;
            if (!userRepository.existsByNickname(candidateNickname)) {
                return candidateNickname;
            }
        }
        
        // 999번 시도해도 실패한 경우 타임스탬프 기반
        return "사용자" + System.currentTimeMillis() % 100000;
    }
    
    private String normalizeNickname(String nickname) {
        if (nickname.length() < 2) {
            return "사용자";
        }
        return nickname.length() > 8 ? nickname.substring(0, 8) : nickname;
    }
}