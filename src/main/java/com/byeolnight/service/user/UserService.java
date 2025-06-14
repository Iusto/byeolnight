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

    public Long register(UserSignUpRequestDto dto, String ipAddress) {
        try {
            if (userRepository.existsByEmail(dto.getEmail())) {
                auditSignupLogRepository.save(
                        AuditSignupLog.failure(dto.getEmail(), ipAddress, "중복된 이메일")
                );
                throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
            }

            if (userRepository.existsByNickname(dto.getNickname())) {
                auditSignupLogRepository.save(
                        AuditSignupLog.failure(dto.getEmail(), ipAddress, "중복된 닉네임")
                );
                throw new DuplicateNicknameException("이미 사용 중인 닉네임입니다.");
            }

            if (!dto.getPassword().equals(dto.getConfirmPassword())) {
                auditSignupLogRepository.save(
                        AuditSignupLog.failure(dto.getEmail(), ipAddress, "비밀번호 불일치")
                );
                throw new PasswordMismatchException("비밀번호가 일치하지 않습니다.");
            }

            if (!isValidPassword(dto.getPassword())) {
                auditSignupLogRepository.save(
                        AuditSignupLog.failure(dto.getEmail(), ipAddress, "비밀번호 정책 위반")
                );
                throw new IllegalArgumentException("비밀번호는 8자 이상이며, 영문/숫자/특수문자를 포함해야 합니다.");
            }

            // 정상 가입 처리
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

            auditSignupLogRepository.save(
                    AuditSignupLog.success(dto.getEmail(), ipAddress)
            );

            return user.getId();

        } catch (RuntimeException e) {
            if (!(e instanceof DuplicateEmailException || e instanceof DuplicateNicknameException
                    || e instanceof PasswordMismatchException || e instanceof IllegalArgumentException)) {
                auditSignupLogRepository.save(
                        AuditSignupLog.failure(dto.getEmail(), ipAddress, "기타 오류: " + e.getMessage())
                );
            }
            throw e;
        }
    }

    private boolean isValidPassword(String password) {
        return password.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$");
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        // ✅ 현재 비밀번호 검증
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new PasswordMismatchException("비밀번호가 일치하지 않습니다.");
        }

        // ✅ 닉네임 중복 검사
        if (!user.getNickname().equals(dto.getNickname()) &&
                userRepository.existsByNickname(dto.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // ✅ 정보 업데이트
        user.updateNickname(dto.getNickname(), LocalDateTime.now());
        user.updatePhone(dto.getPhone());
    }


    @Transactional
    public void updateNickname(Long userId, String newNickname, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        String previousNickname = user.getNickname();

        user.updateNickname(newNickname, LocalDateTime.now());

        NicknameChangeHistory history = NicknameChangeHistory.create(user, previousNickname, newNickname, ipAddress);
        nicknameChangeHistoryRepository.save(history);
    }

    @Transactional
    public void withdraw(Long userId, String password, String reason) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new PasswordMismatchException("비밀번호가 일치하지 않습니다.");
            }

            user.withdraw(reason);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

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
     * 비밀번호 일치 여부 검증
     */
    public boolean checkPassword(String rawPassword, User user) {
        System.out.println("🔍 입력된 비밀번호: " + rawPassword);
        System.out.println("🔐 DB 비밀번호 해시: " + user.getPassword());
        System.out.println("✅ 매치 결과: " + passwordEncoder.matches(rawPassword, user.getPassword()));
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

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
     * 로그인 실패 처리
     * - 실패 횟수 증가 및 잠금 여부 판단
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

    @Transactional(readOnly = true)
    public List<UserSummaryDto> getAllUserSummaries() {
        return userRepository.findAll().stream()
                .map(UserSummaryDto::from)
                .toList();
    }

}