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
     * 매일 오전 10시에 탈퇴 후 5년 경과한 회원 정리
     */
    @Scheduled(cron = "0 0 10 * * *")
    @Transactional
    public void cleanupWithdrawnUsers() {
        LocalDateTime fiveYearsAgo = LocalDateTime.now().minusYears(5);
        
        // 탈퇴 및 밴 계정 모두 5년 후 삭제
        List<User> expiredUsers = userRepository.findByWithdrawnAtBeforeAndStatusIn(
            fiveYearsAgo, List.of(User.UserStatus.WITHDRAWN, User.UserStatus.BANNED));
        
        if (expiredUsers.isEmpty()) {
            log.info("정리할 탈퇴 회원이 없습니다.");
            return;
        }
        
        int cleanedCount = 0;
        for (User user : expiredUsers) {
            try {
                // 개인정보 완전 삭제
                user.completelyRemovePersonalInfo();
                userRepository.save(user); // 즉시 저장로 개별 처리
                cleanedCount++;
                log.info("계정 개인정보 완전 삭제 완료: ID={}, 상태={}, 처리일={}", 
                    user.getId(), user.getStatus(), user.getWithdrawnAt());
            } catch (Exception e) {
                log.error("탈퇴 회원 정리 중 오류 발생: ID={}, 오류={}", 
                    user.getId(), e.getMessage(), e);
                // 개별 오류는 전체 작업을 중단시키지 않음
            }
        }
        
        log.info("만료 계정 정리 완료: {}명 처리 (탈퇴/밴 계정 5년 경과)", cleanedCount);
    }
}