package com.byeolnight.service.user;

import com.byeolnight.dto.user.PasswordChangeRequestDto;
import com.byeolnight.dto.user.UserSignUpRequestDto;
import com.byeolnight.entity.log.AuditSignupLog;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.exception.DuplicateEmailException;
import com.byeolnight.infrastructure.exception.DuplicateNicknameException;
import com.byeolnight.infrastructure.exception.PasswordMismatchException;
import com.byeolnight.repository.log.AuditSignupLogRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.service.auth.EmailAuthService;
import com.byeolnight.service.auth.SocialRevokeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 회원가입, 비밀번호 변경, 탈퇴 등 계정 생명주기만 담당한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserRepository userRepository;
    private final AuditSignupLogRepository auditSignupLogRepository;
    private final UserSecurityService userSecurityService;
    private final EmailAuthService emailAuthService;
    private final UserQueryService userQueryService;
    private final SocialRevokeService socialRevokeService;
    private final DefaultIconService defaultIconService;

    @Transactional
    public Long register(UserSignUpRequestDto dto, String ipAddress) {
        try {
            validateRegistration(dto, ipAddress);
            User savedUser = userRepository.save(User.builder()
                    .email(dto.getEmail())
                    .password(userSecurityService.encodePassword(dto.getPassword()))
                    .nickname(dto.getNickname())
                    .nicknameChanged(false)
                    .nicknameUpdatedAt(LocalDateTime.now())
                    .role(User.Role.USER)
                    .status(User.UserStatus.ACTIVE)
                    .loginFailCount(0)
                    .points(0)
                    .build());

            defaultIconService.grant(savedUser);
            emailAuthService.clearAllEmailData(dto.getEmail());
            auditSignupLogRepository.save(AuditSignupLog.success(dto.getEmail(), ipAddress));
            return savedUser.getId();
        } catch (RuntimeException exception) {
            recordUnexpectedSignupFailure(dto.getEmail(), ipAddress, exception);
            throw exception;
        }
    }

    private void validateRegistration(UserSignUpRequestDto dto, String ipAddress) {
        Optional<User> existingUser = userRepository.findByEmail(dto.getEmail());
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            String reason = user.isSocialUser() ? "소셜 계정 존재" : "중복된 이메일";
            auditSignupLogRepository.save(AuditSignupLog.failure(dto.getEmail(), ipAddress, reason));
            if (user.isSocialUser()) {
                throw new DuplicateEmailException("해당 이메일은 " + user.getSocialProviderName() + " 계정으로 가입되어 있습니다.");
            }
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
        }
        if (isNicknameDuplicated(dto.getNickname())) {
            auditSignupLogRepository.save(AuditSignupLog.failure(dto.getEmail(), ipAddress, "중복된 닉네임"));
            throw new DuplicateNicknameException("이미 사용 중인 닉네임입니다.");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            auditSignupLogRepository.save(AuditSignupLog.failure(dto.getEmail(), ipAddress, "비밀번호 불일치"));
            throw new PasswordMismatchException("비밀번호가 일치하지 않습니다.");
        }
        if (!userSecurityService.isValidPassword(dto.getPassword())) {
            auditSignupLogRepository.save(AuditSignupLog.failure(dto.getEmail(), ipAddress, "비밀번호 정책 위반"));
            throw new IllegalArgumentException("비밀번호는 8자 이상이며 영문, 숫자, 특수문자를 포함해야 합니다.");
        }
        if (!emailAuthService.isAlreadyVerified(dto.getEmail())) {
            auditSignupLogRepository.save(AuditSignupLog.failure(dto.getEmail(), ipAddress, "이메일 인증 미완료"));
            throw new IllegalArgumentException("이메일 인증을 완료해 주세요.");
        }
    }

    private void recordUnexpectedSignupFailure(String email, String ipAddress, RuntimeException exception) {
        if (exception instanceof DuplicateEmailException
                || exception instanceof DuplicateNicknameException
                || exception instanceof PasswordMismatchException
                || exception instanceof IllegalArgumentException) {
            return;
        }
        String message = String.valueOf(exception.getMessage());
        if (message.length() > 450) {
            message = message.substring(0, 450) + "...";
        }
        auditSignupLogRepository.save(AuditSignupLog.failure(email, ipAddress, "기타 오류: " + message));
    }

    public boolean isNicknameDuplicated(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return true;
        }
        return userRepository.existsByNicknameAndStatusNotIn(
                nickname.trim(),
                List.of(User.UserStatus.WITHDRAWN, User.UserStatus.BANNED));
    }

    @Transactional
    public void withdraw(Long userId, String password, String reason) {
        User user = userQueryService.findById(userId);
        if (!user.isSocialUser() && password != null && !password.isEmpty()
                && !userSecurityService.matchesPassword(password, user.getPassword())) {
            throw new PasswordMismatchException("비밀번호가 일치하지 않습니다.");
        }
        if (user.isSocialUser()) {
            try {
                revokeSocialConnection(user);
            } catch (Exception exception) {
                // 외부 제공자 연결 해제 실패는 기록하되 로컬 탈퇴는 계속 진행한다.
                log.warn("소셜 연결 해제 실패 후 로컬 탈퇴 진행: {}", exception.getMessage());
            }
        }
        user.withdraw(reason);
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequestDto dto) {
        User user = userQueryService.findById(userId);
        if (user.isSocialUser()) {
            throw new IllegalArgumentException("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
        }
        if (!userSecurityService.matchesPassword(dto.getCurrentPassword(), user.getPassword())) {
            throw new PasswordMismatchException("현재 비밀번호가 일치하지 않습니다.");
        }
        if (!userSecurityService.isValidPassword(dto.getNewPassword())) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이며 영문, 숫자, 특수문자를 포함해야 합니다.");
        }
        user.changePassword(userSecurityService.encodePassword(dto.getNewPassword()));
    }

    public boolean checkPassword(String rawPassword, User user) {
        return !user.isSocialUser() && userSecurityService.matchesPassword(rawPassword, user.getPassword());
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    private void revokeSocialConnection(User user) {
        String provider = user.getSocialProvider();
        if (provider == null) {
            return;
        }
        switch (provider.toLowerCase()) {
            case "google" -> socialRevokeService.revokeGoogleConnection(user);
            case "kakao" -> socialRevokeService.revokeKakaoConnection(user);
            case "naver" -> socialRevokeService.revokeNaverConnection(user);
            default -> log.warn("지원하지 않는 소셜 제공자: {}", provider);
        }
    }
}
