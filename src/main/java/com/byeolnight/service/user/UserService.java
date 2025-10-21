package com.byeolnight.service.user;

import com.byeolnight.dto.user.MyActivityDto;
import com.byeolnight.dto.user.UpdateProfileRequestDto;
import com.byeolnight.dto.user.UserProfileDto;
import com.byeolnight.dto.user.UserSignUpRequestDto;
import com.byeolnight.dto.user.UserSummaryDto;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * @deprecated 새로운 서비스로 분리됨. UserQueryService, UserProfileService, UserAccountService, UserAdminService 사용
 * 하위 호환성을 위한 Facade 패턴
 */
@Deprecated
@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserQueryService userQueryService;
    private final UserProfileService userProfileService;
    private final UserAccountService userAccountService;
    private final UserAdminService userAdminService;
    private final UserRepository userRepository;

    public Long register(UserSignUpRequestDto dto, String ipAddress) {
        return userAccountService.register(dto, ipAddress);
    }

    public boolean isNicknameDuplicated(String nickname) {
        return userAccountService.isNicknameDuplicated(nickname);
    }

    public Optional<User> findByEmail(String email) {
        return userQueryService.findByEmail(email);
    }

    public User findById(Long userId) {
        return userQueryService.findById(userId);
    }

    public Optional<User> findByNickname(String nickname) {
        return userQueryService.findByNickname(nickname);
    }

    public UserProfileDto getUserProfileByNickname(String nickname) {
        return userProfileService.getUserProfileByNickname(nickname);
    }

    public void updateProfile(Long userId, UpdateProfileRequestDto dto) {
        userProfileService.updateProfile(userId, dto);
    }

    public void updateNickname(Long userId, String newNickname, String ipAddress) {
        userAccountService.updateNickname(userId, newNickname, ipAddress);
    }

    public void withdraw(Long userId, String password, String reason) {
        userAccountService.withdraw(userId, password, reason);
    }

    public void withdraw(Long userId, String reason) {
        userAdminService.withdraw(userId, reason);
    }

    public void requestPasswordReset(String email) {
        userAccountService.requestPasswordReset(email);
    }

    public boolean checkPassword(String rawPassword, User user) {
        return userAccountService.checkPassword(rawPassword, user);
    }

    public void resetPassword(String token, String newPassword) {
        userAccountService.resetPassword(token, newPassword);
    }

    public void changePassword(Long userId, com.byeolnight.dto.user.PasswordChangeRequestDto dto) {
        userAccountService.changePassword(userId, dto);
    }

    public void increaseLoginFailCount(User user, String ipAddress, String userAgent) {
        userAdminService.increaseLoginFailCount(user, ipAddress, userAgent);
    }

    public void resetLoginFailCount(User user) {
        userAdminService.resetLoginFailCount(user);
    }

    public List<UserSummaryDto> getAllUserSummaries() {
        return userAdminService.getAllUserSummaries();
    }

    public void lockUserAccount(Long userId) {
        userAdminService.lockUserAccount(userId);
    }

    public void unlockUserAccount(Long userId) {
        userAdminService.unlockUserAccount(userId);
    }

    public void changeUserStatus(Long userId, User.UserStatus status, String reason) {
        userAdminService.changeUserStatus(userId, status, reason);
    }

    public com.byeolnight.dto.shop.EquippedIconDto getUserEquippedIcon(Long userId) {
        return userProfileService.getUserEquippedIcon(userId);
    }

    public UserProfileDto getUserProfile(Long userId) {
        return userProfileService.getUserProfile(userId);
    }

    public MyActivityDto getMyActivity(Long userId, int page, int size) {
        return userProfileService.getMyActivity(userId, page, size);
    }

    public void grantNicknameChangeTicket(Long userId, Long adminId) {
        userAdminService.grantNicknameChangeTicket(userId, adminId);
    }

    public User save(User user) {
        return userAccountService.save(user);
    }

    public boolean existsByNickname(String nickname) {
        return userQueryService.existsByNickname(nickname);
    }

    public void grantDefaultAsteroidIcon(User user) {
        userAccountService.grantDefaultAsteroidIcon(user);
    }

    @Transactional
    public void migrateDefaultAsteroidIcon() {
        List<User> allUsers = userRepository.findAll();
        int processedCount = 0;
        
        for (User user : allUsers) {
            if (user.getStatus() == User.UserStatus.ACTIVE) {
                grantDefaultAsteroidIcon(user);
                processedCount++;
            }
        }
        
        log.info("기본 소행성 아이콘 마이그레이션 완료: {}명 처리", processedCount);
    }

    @Transactional
    public void createUser(UserSignUpRequestDto dto, jakarta.servlet.http.HttpServletRequest request) {
        String ipAddress = com.byeolnight.infrastructure.util.IpUtil.getClientIp(request);
        register(dto, ipAddress);
    }
}
