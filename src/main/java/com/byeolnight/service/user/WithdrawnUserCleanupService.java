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
 * - 탈퇴 후 2년 경과한 회원 완전 삭제
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawnUserCleanupService {

    private final UserRepository userRepository;

    /**
     * 매일 오전 10시에 탈퇴 후 2년 경과한 회원 완전 삭제
     */
    @Scheduled(cron = "0 0 10 * * *")
    @Transactional
    public void cleanupWithdrawnUsers() {
        LocalDateTime twoYearsAgo = LocalDateTime.now().minusYears(2);

        // 탈퇴 및 밴 계정 모두 2년 후 완전 삭제
        List<User> expiredUsers = userRepository.findByWithdrawnAtBeforeAndStatusIn(
            twoYearsAgo, List.of(User.UserStatus.WITHDRAWN, User.UserStatus.BANNED));
        
        if (expiredUsers.isEmpty()) {
            log.info("완전 삭제할 탈퇴 회원이 없습니다.");
            return;
        }
        
        int deletedCount = 0;
        for (User user : expiredUsers) {
            try {
                userRepository.delete(user);
                deletedCount++;
                log.info("계정 완전 삭제: ID={}, 이메일={}, 탈퇴일={}", 
                    user.getId(), user.getEmail(), user.getWithdrawnAt());
            } catch (Exception e) {
                log.error("탈퇴 회원 삭제 중 오류 발생: ID={}, 오류={}", 
                    user.getId(), e.getMessage(), e);
            }
        }
        
        log.info("탈퇴 계정 완전 삭제 완료: {}명 처리", deletedCount);
    }
}