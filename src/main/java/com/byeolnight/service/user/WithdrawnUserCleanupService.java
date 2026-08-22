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
 * 탈퇴 회원의 보존 기한에 따른 개인정보 정리를 담당한다.
 *
 * <p>탈퇴 후 30일에는 개인정보를 마스킹해 사용자 복구를 차단하고,
 * 2년이 지나면 보존 의무가 끝난 계정을 완전히 삭제한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawnUserCleanupService {

    private final UserRepository userRepository;

    /**
     * 매일 정오에 탈퇴 후 30일이 지난 계정의 이메일과 닉네임을 마스킹한다.
     * 소셜/일반 회원을 구분하지 않고 같은 개인정보 보존 정책을 적용한다.
     */
    @Scheduled(cron = "0 0 12 * * *")
    @Transactional
    public void maskWithdrawnUsersAfterThirtyDays() {
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
                // 이미 마스킹한 계정은 스케줄러가 다시 조회해도 변경하지 않는다.
                if (user.getEmail().startsWith("withdrawn_")) {
                    continue;
                }

                user.maskAfterThirtyDays();
                processedCount++;
                log.info("사용자 마스킹 완료: ID={}, 탈퇴일={}, 타입={}",
                        user.getId(), user.getWithdrawnAt(), user.isSocialUser() ? "소셜" : "일반");
            } catch (Exception e) {
                // 한 계정의 오류 때문에 나머지 정리 대상까지 중단하지 않는다.
                log.error("사용자 마스킹 처리 중 오류 발생: ID={}, 오류={}",
                        user.getId(), e.getMessage(), e);
            }
        }

        log.info("탈퇴 사용자 30일 경과 마스킹 완료: {}명 처리", processedCount);
    }

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
