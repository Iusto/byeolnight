package com.byeolnight.service.user;

import com.byeolnight.domain.entity.log.AuditSignupLog;
import com.byeolnight.domain.entity.log.NicknameChangeHistory;
import com.byeolnight.domain.entity.token.PasswordResetToken;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.AuditSignupLogRepository;
import com.byeolnight.domain.repository.NicknameChangeHistoryRepository;
import com.byeolnight.domain.repository.PasswordResetTokenRepository;
import com.byeolnight.domain.repository.UserRepository;
import com.byeolnight.dto.user.UpdateProfileRequestDto;
import com.byeolnight.dto.user.UserSignUpRequestDto;
import com.byeolnight.dto.user.UserSummaryDto;
import com.byeolnight.infrastructure.exception.DuplicateEmailException;
import com.byeolnight.infrastructure.exception.DuplicateNicknameException;
import com.byeolnight.infrastructure.exception.NotFoundException;
import com.byeolnight.infrastructure.exception.PasswordMismatchException;
import com.byeolnight.service.auth.GmailEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 사용자 관련 비즈니스 로직 처리 서비스
 * - 회원가입, 프로필 수정, 비밀번호 재설정, 로그인 실패 처리 등
 * - 보안 및 운영 관점의 상세 예외처리 포함
 */
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final NicknameChangeHistoryRepository nicknameChangeHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditSignupLogRepository auditSignupLogRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final StringRedisTemplate redisTemplate;
    private final GmailEmailService gmailEmailService;

    /**
     * 회원가입 처리
     */
    public Long register(UserSignUpRequestDto dto, String ipAddress) {
        try {
            if (userRepository.existsByEmail(dto.getEmail())) {
                auditSignupLogRepository.save(AuditSignupLog.failure(dto.getEmail(), ipAddress, "중복된 이메일"));
                throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
            }
            if (userRepository.existsByNickname(dto.getNickname())) {
                auditSignupLogRepository.save(AuditSignupLog.failure(dto.getEmail(), ipAddress, "중복된 닉네임"));
                throw new DuplicateNicknameException("이미 사용 중인 닉네임입니다.");
            }
            if (!dto.getPassword().equals(dto.getConfirmPassword())) {
                auditSignupLogRepository.save(AuditSignupLog.failure(dto.getEmail(), ipAddress, "비밀번호 불일치"));
                throw new PasswordMismatchException("비밀번호가 일치하지 않습니다.");
            }
            if (!isValidPassword(dto.getPassword())) {
                auditSignupLogRepository.save(AuditSignupLog.failure(dto.getEmail(), ipAddress, "비밀번호 정책 위반"));
                throw new IllegalArgumentException("비밀번호는 8자 이상이며, 영문/숫자/특수문자를 포함해야 합니다.");
            }

            User user = User.builder()
                    .email(dto.getEmail())
                    .password(passwordEncoder.encode(dto.getPassword()))
                    .nickname(dto.getNickname())
                    .phone(dto.getPhone())
                    .nicknameChanged(false)
                    .nicknameUpdatedAt(LocalDateTime.now())
                    .role(User.Role.USER)
                    .status(User.UserStatus.ACTIVE)
                    .emailVerified(false)
                    .phoneVerified(false)
                    .loginFailCount(0)
                    .level(1)
                    .exp(0)
                    .build();
            userRepository.save(user);
            auditSignupLogRepository.save(AuditSignupLog.success(dto.getEmail(), ipAddress));
            return user.getId();
        } catch (RuntimeException e) {
            if (!(e instanceof DuplicateEmailException || e instanceof DuplicateNicknameException
                    || e instanceof PasswordMismatchException || e instanceof IllegalArgumentException)) {
                auditSignupLogRepository.save(
                        AuditSignupLog.failure(dto.getEmail(), ipAddress, "기타 오류: " + e.getMessage()));
            }
            throw e;
        }
    }

    /**
     * 비밀번호 형식 검증
     */
    private boolean isValidPassword(String password) {
        return password.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$");
    }

    /**
     * 이메일로 사용자 조회
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * 사용자 프로필 정보 수정 (닉네임/전화번호)
     */
    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new PasswordMismatchException("비밀번호가 일치하지 않습니다.");
        }
        if (!user.getNickname().equals(dto.getNickname()) &&
                userRepository.existsByNickname(dto.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        user.updateNickname(dto.getNickname(), LocalDateTime.now());
        user.updatePhone(dto.getPhone());
    }

    /**
     * 닉네임 변경 처리 및 이력 저장
     */
    @Transactional
    public void updateNickname(Long userId, String newNickname, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        String previousNickname = user.getNickname();
        user.updateNickname(newNickname, LocalDateTime.now());
        NicknameChangeHistory history = NicknameChangeHistory.create(user, previousNickname, newNickname, ipAddress);
        nicknameChangeHistoryRepository.save(history);
    }

    /**
     * 회원 탈퇴 처리
     */
    @Transactional
    public void withdraw(Long userId, String password, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new PasswordMismatchException("비밀번호가 일치하지 않습니다.");
        }
        user.withdraw(reason);
    }

    /**
     * 관리자에 의한 강제 탈퇴 처리
     */
    @Transactional
    public void withdraw(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        user.withdraw(reason);
    }

    /**
     * 비밀번호 재설정 요청 처리
     */
    @Transactional
    public void requestPasswordReset(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("존재하지 않는 이메일입니다.");
        }
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.create(email, token, Duration.ofMinutes(30));
        passwordResetTokenRepository.save(resetToken);
        String resetLink = "https://byeolnight.com/reset-password?token=" + token;
        gmailEmailService.send(email, "비밀번호 재설정 링크", resetLink);
    }

    /**
     * 비밀번호 검증
     */
    public boolean checkPassword(String rawPassword, User user) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    /**
     * 비밀번호 재설정 처리
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));
        if (!resetToken.isValid()) {
            throw new IllegalStateException("만료되었거나 이미 사용된 토큰입니다.");
        }
        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        user.changePassword(passwordEncoder.encode(newPassword));
        resetToken.markAsUsed();
    }

    /**
     * 로그인 실패 시 실패 횟수 증가
     */
    @Transactional
    public void increaseLoginFailCount(User user) {
        user.loginFail();
        userRepository.save(user);
    }

    /**
     * 로그인 성공 시 실패 횟수 초기화
     */
    @Transactional
    public void resetLoginFailCount(User user) {
        user.loginSuccess();
        userRepository.save(user);
    }

    /**
     * 관리자 - 전체 사용자 요약 정보 조회
     */
    @Transactional(readOnly = true)
    public List<UserSummaryDto> getAllUserSummaries() {
        return userRepository.findAll().stream()
                .map(UserSummaryDto::from)
                .toList();
    }

    /**
     * 관리자 - 사용자 계정 잠금 처리
     */
    @Transactional
    public void lockUserAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        user.lockAccount();
    }

    /**
     * 관리자 - 사용자 계정 상태 변경
     */
    @Transactional
    public void changeUserStatus(Long userId, User.UserStatus status, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        switch (status) {
            case BANNED -> user.ban(reason);
            case ACTIVE -> user.unban(); // 이미 존재하는 도메인 메서드
            case SUSPENDED -> user.changeStatus(User.UserStatus.SUSPENDED);
            default -> throw new IllegalArgumentException("허용되지 않은 상태 변경입니다.");
        }
    }
}