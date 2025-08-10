package com.byeolnight.service.user;

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
 * 탈퇴 회원 정리 서비스
 * - 탈퇴 후 5년 경과한 회원의 개인정보 완전 삭제
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawnUserCleanupService {

    private final UserRepository userRepository;

    /**
     * 매일 새벽 3시에 탈퇴 후 5년 경과한 회원 정리
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupWithdrawnUsers() {
        LocalDateTime fiveYearsAgo = LocalDateTime.now().minusYears(5);
        
        List<User> expiredUsers = userRepository.findByStatusAndWithdrawnAtBefore(
            User.UserStatus.WITHDRAWN, fiveYearsAgo);
        
        if (expiredUsers.isEmpty()) {
            log.info("정리할 탈퇴 회원이 없습니다.");
            return;
        }
        
        int cleanedCount = 0;
        for (User user : expiredUsers) {
            try {
                // 개인정보 완전 삭제
                user.completelyRemovePersonalInfo();
                cleanedCount++;
                log.info("탈퇴 회원 개인정보 완전 삭제 완료: ID={}, 탈퇴일={}", 
                    user.getId(), user.getWithdrawnAt());
            } catch (Exception e) {
                log.error("탈퇴 회원 정리 중 오류 발생: ID={}, 오류={}", 
                    user.getId(), e.getMessage(), e);
            }
        }
        
        log.info("탈퇴 회원 정리 완료: {}명 처리", cleanedCount);
    }
}