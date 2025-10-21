package com.byeolnight.service.user;

import com.byeolnight.dto.user.UserSummaryDto;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.exception.NotFoundException;
import com.byeolnight.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자 기능 서비스
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final UserQueryService userQueryService;
    private final UserSecurityService userSecurityService;

    public List<UserSummaryDto> getAllUserSummaries() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() != User.Role.ADMIN)
                .map(UserSummaryDto::from)
                .toList();
    }

    @Transactional
    public void lockUserAccount(Long userId) {
        User user = userQueryService.findById(userId);
        user.lockAccount();
    }

    @Transactional
    public void unlockUserAccount(Long userId) {
        User user = userQueryService.findById(userId);
        user.unlockAccount();
    }

    @Transactional
    public void changeUserStatus(Long userId, User.UserStatus status, String reason) {
        User user = userQueryService.findById(userId);

        switch (status) {
            case BANNED:
                user.ban(reason);
                break;
            case ACTIVE:
                user.unban();
                break;
            case SUSPENDED:
                user.changeStatus(User.UserStatus.SUSPENDED);
                break;
            default:
                throw new IllegalArgumentException("허용되지 않은 상태 변경입니다.");
        }
    }

    @Transactional
    public void grantNicknameChangeTicket(Long userId, Long adminId) {
        User user = userQueryService.findById(userId);
        User admin = userQueryService.findById(adminId);
        
        user.resetNicknameChangeRestriction();
        log.info("관리자 {}가 사용자 {}에게 닉네임 변경권을 수여했습니다.", admin.getNickname(), user.getNickname());
    }

    @Transactional
    public void withdraw(Long userId, String reason) {
        User user = userQueryService.findById(userId);
        user.withdraw(reason);
    }

    @Transactional
    public void increaseLoginFailCount(User user, String ipAddress, String userAgent) {
        userSecurityService.handleLoginFailure(user, ipAddress, userAgent);
        userRepository.save(user);
    }

    @Transactional
    public void resetLoginFailCount(User user) {
        user.loginSuccess();
        userRepository.save(user);
    }
}
