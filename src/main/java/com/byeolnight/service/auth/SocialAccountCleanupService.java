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
                log.info("소셜 연동 해제 탈퇴 신청 (30일 복구 가능): {} ({})", email, provider);
            }
        });
    }
    
    /**
     * 탈퇴 후 30일 경과한 모든 사용자 이메일 마스킹 처리
     */
    @Scheduled(cron = "0 0 12 * * *") // 매일 정오마다 실행
    @Transactional
    public void maskEmailAfterThirtyDays() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        // 탈퇴 후 30일 경과한 모든 사용자 조회 (이메일이 아직 마스킹되지 않은 경우)
        List<User> expiredUsers = userRepository.findByStatusAndWithdrawnAtBefore(
            User.UserStatus.WITHDRAWN, thirtyDaysAgo);
        
        if (expiredUsers.isEmpty()) {
            log.info("30일 경과 탈퇴 사용자가 없습니다.");
            return;
        }
        
        int processedCount = 0;
        for (User user : expiredUsers) {
            try {
                // 이미 마스킹된 이메일은 건너뛰기
                if (user.getEmail().startsWith("withdrawn_") || user.getEmail().startsWith("deleted_")) {
                    continue;
                }
                
                // 소셜 사용자인 경우 연동 해제
                if (user.isSocialUser()) {
                    revokeSocialConnection(user);
                }
                
                // 이메일 마스킹 처리
                user.maskEmailAfterThirtyDays();
                
                processedCount++;
                log.info("사용자 이메일 마스킹 완료: ID={}, 이메일={}, 소셜여부={}, 탈퇴일={}", 
                    user.getId(), user.getEmail(), user.isSocialUser(), user.getWithdrawnAt());
                    
            } catch (Exception e) {
                log.error("사용자 이메일 마스킹 처리 중 오류 발생: ID={}, 오류={}", 
                    user.getId(), e.getMessage(), e);
            }
        }
        
        log.info("사용자 30일 경과 이메일 마스킹 완료: {}명 처리", processedCount);
    }

    /**
     * 매일 오전 9시 - 5년 경과 계정 개인정보 완전 마스킹 (완전 삭제 준비)
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void completelyMaskPersonalInfoAfterFiveYears() {
        log.info("🔒 5년 경과 계정 개인정보 완전 마스킹 작업 시작");
        
        try {
            LocalDateTime fiveYearsAgo = LocalDateTime.now().minusYears(5);
            List<User> expiredUsers = userRepository.findByStatusAndWithdrawnAtBefore(
                User.UserStatus.WITHDRAWN, fiveYearsAgo);
            
            int maskedCount = 0;
            for (User user : expiredUsers) {
                // 이미 완전 마스킹된 계정은 건너뛰기
                if (!user.getEmail().startsWith("deleted_")) {
                    user.completelyRemovePersonalInfo();
                    maskedCount++;
                    log.info("개인정보 완전 마스킹 완료: {}", user.getEmail());
                }
            }
            
            log.info("🔒 개인정보 완전 마스킹 완료: {}개 계정 처리", maskedCount);
            
        } catch (Exception e) {
            log.error("개인정보 완전 마스킹 작업 중 오류 발생", e);
        }
    }

    /**
     * 매일 오전 10시 - 10년 경과 모든 계정 완전 삭제
     */
    @Scheduled(cron = "0 0 10 * * *")
    @Transactional
    public void cleanupWithdrawnAccounts() {
        log.info("🧹 10년 경과 모든 계정 완전 삭제 작업 시작");
        
        try {
            LocalDateTime tenYearsAgo = LocalDateTime.now().minusYears(10);
            List<User> withdrawnUsers = userRepository.findByStatusAndWithdrawnAtBefore(
                User.UserStatus.WITHDRAWN, tenYearsAgo);
            
            int deletedCount = 0;
            for (User user : withdrawnUsers) {
                userRepository.delete(user);
                deletedCount++;
                log.info("계정 완전 삭제: {}", user.getEmail());
            }
            
            log.info("🧹 계정 완전 삭제 완료: {}개 계정 처리", deletedCount);
            
        } catch (Exception e) {
            log.error("계정 완전 삭제 작업 중 오류 발생", e);
        }
    }

    /**
     * 복구 가능한 계정이 있는지 확인 (복구하지 않음)
     */
    @Transactional(readOnly = true)
    public boolean hasRecoverableAccount(String email) {
        return canRecover(email);
    }

    /**
     * 소셜 사용자 탈퇴 복구 처리 (30일 내에만 가능)
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
        
        // 소셜 사용자가 아니면 복구 불가
        if (!user.isSocialUser()) {
            log.warn("복구 시도 실패: 소셜 사용자가 아님 - {}", email);
            return false;
        }
        
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
            
            // 이메일 기반 고유 닉네임 생성 (복구 시에는 제한 무시)
            String newNickname = generateUniqueNicknameFromEmail(email);
            user.forceUpdateNickname(newNickname);
            
            log.info("소셜 계정 복구 완료: 이메일={}, 새 닉네임={}, 제공자={}", 
                email, newNickname, user.getSocialProvider());
            
            return true;
            
        } catch (IllegalStateException e) {
            // 닉네임 변경 제한 오류는 이미 forceUpdateNickname으로 해결됨
            log.error("소셜 계정 복구 중 예상치 못한 오류: 이메일={}, 오류={}", email, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("소셜 계정 복구 중 오류 발생: 이메일={}, 오류={}", email, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 30일 내 복구 가능 여부 확인
     */
    public boolean canRecover(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return false;
        }
        
        User user = userOpt.get();
        
        // 소셜 사용자가 아니면 복구 불가
        if (!user.isSocialUser()) {
            return false;
        }
        
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
    
    /**
     * 소셜 연동 해제 처리
     */
    private void revokeSocialConnection(User user) {
        try {
            SocialRevokeService socialRevokeService = 
                com.byeolnight.infrastructure.config.ApplicationContextProvider.getBean(SocialRevokeService.class);
            
            String provider = user.getSocialProvider();
            if (provider != null) {
                switch (provider.toLowerCase()) {
                    case "google" -> socialRevokeService.revokeGoogleConnection(user);
                    case "kakao" -> socialRevokeService.revokeKakaoConnection(user);
                    case "naver" -> socialRevokeService.revokeNaverConnection(user);
                    default -> log.warn("지원하지 않는 소셜 플랫폼: {}", provider);
                }
            }
        } catch (Exception e) {
            log.error("소셜 연동 해제 실패 - {}: {}", user.getSocialProvider(), e.getMessage());
            // 연동 해제 실패해도 계속 진행
        }
    }
}