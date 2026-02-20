package com.byeolnight.service.user;

import com.byeolnight.dto.user.UserSignUpRequestDto;
import com.byeolnight.entity.log.AuditSignupLog;
import com.byeolnight.entity.log.NicknameChangeHistory;
import com.byeolnight.entity.shop.StellaIcon;
import com.byeolnight.entity.shop.UserIcon;
import com.byeolnight.entity.token.PasswordResetToken;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.config.ApplicationContextProvider;
import com.byeolnight.infrastructure.exception.*;
import com.byeolnight.repository.PasswordResetTokenRepository;
import com.byeolnight.repository.log.AuditSignupLogRepository;
import com.byeolnight.repository.log.NicknameChangeHistoryRepository;
import com.byeolnight.repository.shop.StellaIconRepository;
import com.byeolnight.repository.shop.UserIconRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.service.auth.EmailAuthService;
import com.byeolnight.service.auth.GmailEmailService;
import com.byeolnight.service.auth.SocialRevokeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 사용자 계정 관리 서비스 (회원가입, 탈퇴, 비밀번호)
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class UserAccountService {

    private final UserRepository userRepository;
    private final NicknameChangeHistoryRepository nicknameChangeHistoryRepository;
    private final AuditSignupLogRepository auditSignupLogRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final StellaIconRepository stellaIconRepository;
    private final UserIconRepository userIconRepository;
    private final GmailEmailService gmailEmailService;
    private final UserSecurityService userSecurityService;
    private final EmailAuthService emailAuthService;
    private final UserQueryService userQueryService;

    @Transactional
    public Long register(UserSignUpRequestDto dto, String ipAddress) {
        try {
            Optional<User> existingUser = userRepository.findByEmail(dto.getEmail());
            if (existingUser.isPresent()) {
                User user = existingUser.get();
                if (user.isSocialUser()) {
                    auditSignupLogRepository.save(AuditSignupLog.failure(dto.getEmail(), ipAddress, "소셜 계정 존재"));
                    throw new DuplicateEmailException("해당 이메일로 소셜 계정(" + user.getSocialProviderName() + ")이 존재합니다. 소셜 로그인을 이용해주세요.");
                } else {
                    auditSignupLogRepository.save(AuditSignupLog.failure(dto.getEmail(), ipAddress, "중복된 이메일"));
                    throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
                }
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
                throw new IllegalArgumentException("비밀번호는 8자 이상이며, 영문/숫자/특수문자를 포함해야 합니다.");
            }
            
            if (!emailAuthService.isAlreadyVerified(dto.getEmail())) {
                auditSignupLogRepository.save(AuditSignupLog.failure(dto.getEmail(), ipAddress, "이메일 인증 미완료"));
                throw new IllegalArgumentException("이메일 인증을 완료해주세요.");
            }

            User user = User.builder()
                    .email(dto.getEmail())
                    .password(userSecurityService.encodePassword(dto.getPassword()))
                    .nickname(dto.getNickname())
                    .nicknameChanged(false)
                    .nicknameUpdatedAt(LocalDateTime.now())
                    .role(User.Role.USER)
                    .status(User.UserStatus.ACTIVE)
                    .loginFailCount(0)
                    .points(0)
                    .build();
            User savedUser = userRepository.save(user);
            
            grantDefaultAsteroidIcon(savedUser);
            emailAuthService.clearAllEmailData(dto.getEmail());
            
            auditSignupLogRepository.save(AuditSignupLog.success(dto.getEmail(), ipAddress));
            return user.getId();
        } catch (RuntimeException e) {
            if (!(e instanceof DuplicateEmailException || e instanceof DuplicateNicknameException
                    || e instanceof PasswordMismatchException || e instanceof IllegalArgumentException)) {
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

    public boolean isNicknameDuplicated(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return true;
        }
        
        String trimmedNickname = nickname.trim();
        boolean exists = userRepository.existsByNicknameAndStatusNotIn(
            trimmedNickname, 
            List.of(User.UserStatus.WITHDRAWN, User.UserStatus.BANNED)
        );
        
        log.info("[🔍 닉네임 중복 검사] 입력값: '{}', 정리된 값: '{}', 중복 여부: {} (탈퇴/밴 계정 제외)", nickname, trimmedNickname, exists);
        return exists;
    }

    @Transactional
    public void updateNickname(Long userId, String newNickname, String ipAddress) {
        User user = userQueryService.findById(userId);
        String previousNickname = user.getNickname();
        user.updateNickname(newNickname, LocalDateTime.now());
        NicknameChangeHistory history = NicknameChangeHistory.create(user, previousNickname, newNickname, ipAddress);
        nicknameChangeHistoryRepository.save(history);
    }

    @Transactional
    public void withdraw(Long userId, String password, String reason) {
        User user = userQueryService.findById(userId);
        
        if (!user.isSocialUser() && password != null && !password.isEmpty() && 
            !userSecurityService.matchesPassword(password, user.getPassword())) {
            throw new PasswordMismatchException("비밀번호가 일치하지 않습니다.");
        }
        
        if (user.isSocialUser()) {
            try {
                revokeSocialConnection(user);
            } catch (Exception e) {
                log.warn("소셜 연동 해제 실패 (계속 진행): {}", e.getMessage());
            }
        }
        
        user.withdraw(reason);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EmailNotFoundException("존재하지 않는 이메일입니다."));
        
        if (user.isSocialUser()) {
            throw new IllegalArgumentException("소셜 로그인 사용자는 비밀번호 재설정을 사용할 수 없습니다.");
        }
        
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.create(email, token, Duration.ofMinutes(30));
        passwordResetTokenRepository.save(resetToken);
        String resetLink = "https://byeolnight.com/reset-password?token=" + token;
        String subject = "별 헤는 밤 - 비밀번호 재설정 안내";

        String content = """
        <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
            <h2>안녕하세요, 별 헤는 밤입니다 🌌</h2>
            <p>비밀번호 재설정을 요청하셨습니다.</p>
            <p>아래 버튼을 클릭하여 비밀번호를 재설정해 주세요. 이 링크는 <strong>30분간만 유효</strong>합니다.</p>
            <div style="margin: 30px 0;">
                <a href="%s" style="background-color: #4a90e2; color: white; padding: 12px 20px; text-decoration: none; border-radius: 5px;">비밀번호 재설정하기</a>
            </div>
            <p>만약 본인이 요청하지 않으셨다면 이 메일을 무시하셔도 됩니다.</p>
            <hr style="border: none; border-top: 1px solid #ccc;" />
            <p style="font-size: 0.9em; color: #888;">© 2025 별 헤는 밤 | byeolnight.com</p>
        </div>
    """.formatted(resetLink);
        gmailEmailService.sendHtml(email, subject, content);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        if (!resetToken.isValid()) {
            throw new ExpiredResetTokenException("만료되었거나 이미 사용된 토큰입니다.");
        }

        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        user.changePassword(userSecurityService.encodePassword(newPassword));
        user.loginSuccess();
        resetToken.markAsUsed();
    }

    @Transactional
    public void changePassword(Long userId, com.byeolnight.dto.user.PasswordChangeRequestDto dto) {
        User user = userQueryService.findById(userId);
        
        if (user.isSocialUser()) {
            throw new IllegalArgumentException("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
        }
        
        if (!userSecurityService.matchesPassword(dto.getCurrentPassword(), user.getPassword())) {
            throw new PasswordMismatchException("현재 비밀번호가 일치하지 않습니다.");
        }
        
        if (!userSecurityService.isValidPassword(dto.getNewPassword())) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이며, 영문/숫자/특수문자를 포함해야 합니다.");
        }
        
        user.changePassword(userSecurityService.encodePassword(dto.getNewPassword()));
    }

    public boolean checkPassword(String rawPassword, User user) {
        if (user.isSocialUser()) {
            return false;
        }
        return userSecurityService.matchesPassword(rawPassword, user.getPassword());
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void grantDefaultAsteroidIcon(User user) {
        try {
            StellaIcon asteroidIcon = stellaIconRepository.findByName("소행성")
                    .or(() -> stellaIconRepository.findByName("Asteroid"))
                    .orElse(null);
            
            if (asteroidIcon == null) {
                log.warn("소행성 아이콘을 찾을 수 없습니다. 기본 아이콘 부여를 건너뜁니다.");
                return;
            }
            
            boolean alreadyOwns = userIconRepository.existsByUserAndStellaIcon(user, asteroidIcon);
            
            if (!alreadyOwns) {
                UserIcon userIcon = UserIcon.builder()
                        .user(user)
                        .stellaIcon(asteroidIcon)
                        .purchasePrice(0)
                        .build();
                userIconRepository.save(userIcon);
                log.info("사용자 {}에게 기본 소행성 아이콘 부여 완료", user.getNickname());
            }
            
            if (user.getEquippedIconId() == null) {
                user.equipIcon(asteroidIcon.getId(), asteroidIcon.getIconUrl());
                userRepository.save(user);
                log.info("사용자 {}에게 기본 소행성 아이콘 장착 완료", user.getNickname());
            }
            
        } catch (Exception e) {
            log.error("기본 소행성 아이콘 부여 중 오류 발생: {}", e.getMessage(), e);
        }
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

    private void revokeSocialConnection(User user) {
        String provider = user.getSocialProvider();
        if (provider == null) return;
        
        try {
            SocialRevokeService socialRevokeService =
                ApplicationContextProvider.getBean(SocialRevokeService.class);
            
            switch (provider.toLowerCase()) {
                case "google" -> socialRevokeService.revokeGoogleConnection(user);
                case "kakao" -> socialRevokeService.revokeKakaoConnection(user);
                case "naver" -> socialRevokeService.revokeNaverConnection(user);
                default -> log.warn("지원하지 않는 소셜 플랫폼: {}", provider);
            }
        } catch (Exception e) {
            log.error("소셜 연동 해제 실패 - {}: {}", provider, e.getMessage());
            throw e;
        }
    }
}
