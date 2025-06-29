package com.byeolnight.service.user;

import com.byeolnight.domain.entity.log.AuditSignupLog;
import com.byeolnight.domain.entity.log.NicknameChangeHistory;
import com.byeolnight.domain.entity.token.PasswordResetToken;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.log.AuditSignupLogRepository;
import com.byeolnight.domain.repository.log.NicknameChangeHistoryRepository;
import com.byeolnight.domain.repository.PasswordResetTokenRepository;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.dto.user.UpdateProfileRequestDto;
import com.byeolnight.dto.user.UserSignUpRequestDto;
import com.byeolnight.dto.user.UserSummaryDto;
import com.byeolnight.infrastructure.exception.*;
import com.byeolnight.service.auth.GmailEmailService;
import lombok.RequiredArgsConstructor;
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
    private final GmailEmailService gmailEmailService;
    private final UserSecurityService userSecurityService;
    private final com.byeolnight.domain.repository.post.PostRepository postRepository;
    private final com.byeolnight.domain.repository.CommentRepository commentRepository;

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
            if (!userSecurityService.isValidPassword(dto.getPassword())) {
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
                    .points(0)
                    .build();
            userRepository.save(user);
            auditSignupLogRepository.save(AuditSignupLog.success(dto.getEmail(), ipAddress));
            return user.getId();
        } catch (RuntimeException e) {
            if (!(e instanceof DuplicateEmailException || e instanceof DuplicateNicknameException
                    || e instanceof PasswordMismatchException || e instanceof IllegalArgumentException)) {
                // 오류 메시지 길이 제한 (500자)
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.length() > 450) {
                    errorMessage = errorMessage.substring(0, 450) + "...";
                }
                auditSignupLogRepository.save(
                        AuditSignupLog.failure(dto.getEmail(), ipAddress, "기타 오류: " + errorMessage));
            }
            throw e;
        }
    }

    /**
     * 닉네임 중복 검사
     */
    public boolean isNicknameDuplicated(String nickname) {
        return userRepository.existsByNickname(nickname);
    }



    /**
     * 이메일로 사용자 조회
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * ID로 사용자 조회
     */
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }

    /**
     * 닉네임으로 사용자 조회
     */
    public Optional<User> findByNickname(String nickname) {
        return userRepository.findByNickname(nickname);
    }

    /**
     * 닉네임으로 사용자 프로필 조회
     */
    @Transactional(readOnly = true)
    public com.byeolnight.dto.user.UserProfileDto getUserProfileByNickname(String nickname) {
        System.out.println("프로필 조회 요청: " + nickname); // 디버그용 로그
        
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> {
                    System.out.println("사용자를 찾을 수 없음: " + nickname);
                    return new NotFoundException("사용자를 찾을 수 없습니다.");
                });
        
        System.out.println("사용자 찾음: " + user.getNickname() + ", ID: " + user.getId());
        
        // 게시글 수와 댓글 수 조회
        long postCount = postRepository.countByWriterAndIsDeletedFalse(user);
        long commentCount = commentRepository.countByWriter(user);
        
        System.out.println("게시글 수: " + postCount + ", 댓글 수: " + commentCount);
        
        return com.byeolnight.dto.user.UserProfileDto.from(user, postCount, commentCount);
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
        
        // 닉네임이 변경된 경우에만 검증 수행
        if (!user.getNickname().equals(dto.getNickname())) {
            // 중복 닉네임 검사
            if (userRepository.existsByNickname(dto.getNickname())) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            }
            
            // 6개월 제한 검사
            if (user.isNicknameChanged() && user.getNicknameUpdatedAt() != null &&
                    user.getNicknameUpdatedAt().isAfter(LocalDateTime.now().minusMonths(6))) {
                throw new IllegalArgumentException("닉네임은 6개월마다 변경할 수 있습니다. 다음 변경 가능 시기: " + 
                        user.getNicknameUpdatedAt().plusMonths(6).toLocalDate());
            }
            
            user.updateNickname(dto.getNickname(), LocalDateTime.now());
        }
        
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
            throw new EmailNotFoundException("존재하지 않는 이메일입니다.");
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
            throw new ExpiredResetTokenException("만료되었거나 이미 사용된 토큰입니다.");
        }

        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        // ✅ 비밀번호 변경
        user.changePassword(passwordEncoder.encode(newPassword));

        // ✅ 계정 잠금 해제 및 실패 횟수 초기화
        user.loginSuccess();  // 내부적으로 failCount 초기화 + 잠금 해제 + 마지막 로그인 시각 갱신

        // ✅ 비밀번호 재설정에 사용된 토큰을 더 이상 재사용하지 못하게 처리
        resetToken.markAsUsed();
    }

    /**
     * 로그인 실패 시 실패 횟수 증가 + 보안 정책 적용
     */
    @Transactional
    public void increaseLoginFailCount(User user, String ipAddress, String userAgent) {
        userSecurityService.handleLoginFailure(user, ipAddress, userAgent);
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
     * 관리자 - 전체 사용자 요약 정보 조회 (관리자 제외)
     */
    @Transactional(readOnly = true)
    public List<UserSummaryDto> getAllUserSummaries() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() != User.Role.ADMIN) // 관리자 제외
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
     * 관리자 - 사용자 계정 잠금해제 처리
     */
    @Transactional
    public void unlockUserAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        user.unlockAccount();
    }

    /**
     * 관리자 - 사용자 계정 상태 변경
     */
    @Transactional
    public void changeUserStatus(Long userId, User.UserStatus status, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

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

    /**
     * 비밀번호 변경
     */
    @Transactional
    public void changePassword(Long userId, com.byeolnight.dto.user.PasswordChangeRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        
        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new PasswordMismatchException("현재 비밀번호가 일치하지 않습니다.");
        }
        
        // 새 비밀번호 유효성 검사
        if (!userSecurityService.isValidPassword(dto.getNewPassword())) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이며, 영문/숫자/특수문자를 포함해야 합니다.");
        }
        
        // 비밀번호 변경
        user.changePassword(passwordEncoder.encode(dto.getNewPassword()));
    }

    /**
     * 특정 사용자의 장착 중인 아이콘 조회
     */
    public com.byeolnight.dto.shop.EquippedIconDto getUserEquippedIcon(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getEquippedIconId() == null) {
            return null;
        }

        // StellaIconRepository를 직접 주입받지 않고 다른 방법 사용
        return null; // 임시로 null 반환
    }
}