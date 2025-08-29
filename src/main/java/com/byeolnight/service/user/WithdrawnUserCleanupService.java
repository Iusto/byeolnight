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
 * - 탈퇴 후 1년 경과한 회원의 개인정보 마스킹 처리
 * - 외래키 제약조건으로 인해 완전 삭제 대신 마스킹 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawnUserCleanupService {

    private final UserRepository userRepository;

    /**
     * 매일 오전 10시에 탈퇴 후 1년 경과한 회원 마스킹 처리
     */
    @Scheduled(cron = "0 0 10 * * *")
    @Transactional
    public void cleanupWithdrawnUsers() {
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        
        // 탈퇴 및 밴 계정 모두 1년 후 마스킹 처리
        List<User> expiredUsers = userRepository.findByWithdrawnAtBeforeAndStatusIn(
            oneYearAgo, List.of(User.UserStatus.WITHDRAWN, User.UserStatus.BANNED));
        
        if (expiredUsers.isEmpty()) {
            log.info("마스킹 처리할 탈퇴 회원이 없습니다.");
            return;
        }
        
        int cleanedCount = 0;
        for (User user : expiredUsers) {
            try {
                // 개인정보 마스킹 처리 (외래키 제약조건으로 인해 완전 삭제 불가)
                user.completelyRemovePersonalInfo();
                userRepository.save(user);
                cleanedCount++;
                log.info("계정 개인정보 마스킹 완료: ID={}, 상태={}, 처리일={}", 
                    user.getId(), user.getStatus(), user.getWithdrawnAt());
            } catch (Exception e) {
                log.error("탈퇴 회원 정리 중 오류 발생: ID={}, 오류={}", 
                    user.getId(), e.getMessage(), e);
                // 개별 오류는 전체 작업을 중단시키지 않음
            }
        }
        
        log.info("만료 계정 마스킹 완료: {}명 처리 (탈퇴/밴 계정 1년 경과)", cleanedCount);
    }
}