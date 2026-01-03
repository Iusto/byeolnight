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
import java.util.Optional;

/**
 * 계정 정리 서비스
 * - 일반/소셜 사용자 모두 30일 후 마스킹 처리
 * - 일반/소셜 계정 복구 기능 (30일 이내)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialAccountCleanupService {

    private final UserRepository userRepository;


    
    /**
     * 탈퇴 후 30일 경과 시 마스킹 처리 (일반/소셜 모두 적용, 복구 불가능)
     */
    @Scheduled(cron = "0 0 12 * * *")
    @Transactional
    public void maskSocialUsersAfterThirtyDays() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<User> expiredUsers = userRepository.findByStatusAndWithdrawnAtBefore(
            User.UserStatus.WITHDRAWN, thirtyDaysAgo);

        if (expiredUsers.isEmpty()) {
            log.info("30일 경과 탈퇴 사용자가 없습니다.");
            return;
        }

        int processedCount = 0;
        for (User user : expiredUsers) {
            try {
                // 이미 마스킹된 경우 건너뛰기
                if (user.getEmail().startsWith("withdrawn_")) {
                    continue;
                }

                user.maskAfterThirtyDays();
                processedCount++;
                log.info("사용자 마스킹 완료: ID={}, 이메일={}, 탈퇴일={}, 타입={}",
                    user.getId(), user.getEmail(), user.getWithdrawnAt(),
                    user.isSocialUser() ? "소셜" : "일반");

            } catch (Exception e) {
                log.error("사용자 마스킹 처리 중 오류 발생: ID={}, 오류={}",
                    user.getId(), e.getMessage(), e);
            }
        }

        log.info("탈퇴 사용자 30일 경과 마스킹 완료: {}명 처리", processedCount);
    }



    /**
     * 복구 가능한 계정이 있는지 확인 (복구하지 않음)
     */
    @Transactional(readOnly = true)
    public boolean hasRecoverableAccount(String email) {
        return canRecover(email);
    }

    /**
     * 탈퇴 계정 복구 처리 (일반/소셜 모두 30일 내에만 가능)
     * @param email 복구할 사용자 이메일
     * @return 복구 성공 여부
     */
    @Transactional
    public boolean recoverWithdrawnAccount(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            log.warn("복구 시도 실패: 존재하지 않는 이메일 - {}", email);
            return false;
        }

        User user = userOpt.get();

        // 탈퇴 신청을 하지 않은 경우
        if (!user.isWithdrawalRequested()) {
            log.warn("복구 시도 실패: 탈퇴 신청 이력 없음 - {}", email);
            return false;
        }

        // 30일 경과 여부 확인
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        if (user.getWithdrawnAt().isBefore(thirtyDaysAgo)) {
            log.warn("복구 시도 실패: 30일 경과 - 이메일: {}, 탈퇴일: {}", email, user.getWithdrawnAt());
            return false;
        }

        // 이미 이메일이 마스킹된 경우 (30일 경과)
        if (user.getEmail().startsWith("withdrawn_")) {
            log.warn("복구 시도 실패: 이미 이메일 마스킹 처리됨 - {}", email);
            return false;
        }

        // 복구 처리
        try {
            // 탈퇴 정보 초기화 (상태를 ACTIVE로 변경)
            user.clearWithdrawalInfo();

            log.info("계정 복구 완료: 이메일={}, 닉네임={}, 타입={}",
                email, user.getNickname(), user.isSocialUser() ? "소셜(" + user.getSocialProvider() + ")" : "일반");

            return true;

        } catch (Exception e) {
            log.error("계정 복구 중 오류 발생: 이메일={}, 오류={}", email, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 30일 내 복구 가능 여부 확인 (일반/소셜 모두 가능)
     */
    public boolean canRecover(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        // 탈퇴 신청을 하지 않은 경우
        if (!user.isWithdrawalRequested()) {
            return false;
        }

        // 이미 이메일이 마스킹된 경우 (30일 경과)
        if (user.getEmail().startsWith("withdrawn_")) {
            return false;
        }

        // 30일 경과 여부 확인
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return user.getWithdrawnAt().isAfter(thirtyDaysAgo);
    }
    
    /**
     * 이메일 기반 고유 닉네임 생성
     */
    public String generateUniqueNicknameFromEmail(String email) {
        String baseNickname = email.split("@")[0];
        String normalizedNickname = normalizeNickname(baseNickname);
        
        if (!userRepository.existsByNickname(normalizedNickname)) {
            return normalizedNickname;
        }
        
        for (int i = 1; i <= 999; i++) {
            String candidateNickname = normalizedNickname + i;
            if (!userRepository.existsByNickname(candidateNickname)) {
                return candidateNickname;
            }
        }
        
        return "사용자" + System.currentTimeMillis() % 100000;
    }
    
    private String normalizeNickname(String nickname) {
        if (nickname.length() < 2) {
            return "사용자";
        }
        return nickname.length() > 8 ? nickname.substring(0, 8) : nickname;
    }
}