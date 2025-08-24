package com.byeolnight.service.user;

import com.byeolnight.entity.log.AuditSignupLog;
import com.byeolnight.entity.log.NicknameChangeHistory;
import com.byeolnight.entity.shop.StellaIcon;
import com.byeolnight.entity.shop.UserIcon;
import com.byeolnight.entity.token.PasswordResetToken;
import com.byeolnight.entity.user.User;
import com.byeolnight.entity.certificate.UserCertificate;
import com.byeolnight.infrastructure.config.ApplicationContextProvider;
import com.byeolnight.repository.MessageRepository;
import com.byeolnight.repository.comment.CommentRepository;
import com.byeolnight.repository.log.AuditSignupLogRepository;
import com.byeolnight.repository.log.NicknameChangeHistoryRepository;
import com.byeolnight.repository.PasswordResetTokenRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.dto.user.UpdateProfileRequestDto;
import com.byeolnight.dto.user.UserSignUpRequestDto;
import com.byeolnight.dto.user.UserSummaryDto;
import com.byeolnight.dto.user.UserProfileDto;
import com.byeolnight.dto.user.MyActivityDto;
import com.byeolnight.dto.certificate.CertificateDto;
import com.byeolnight.dto.post.PostDto;
import com.byeolnight.dto.comment.CommentDto;
import com.byeolnight.dto.message.MessageDto;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.repository.shop.StellaIconRepository;
import com.byeolnight.repository.shop.UserIconRepository;
import com.byeolnight.service.auth.SocialRevokeService;
import com.byeolnight.service.certificate.CertificateService;
import com.byeolnight.service.post.PostService;
import com.byeolnight.service.comment.CommentService;
import com.byeolnight.service.message.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.byeolnight.infrastructure.exception.*;
import com.byeolnight.service.auth.GmailEmailService;
import com.byeolnight.infrastructure.security.EncryptionUtil;
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
@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final NicknameChangeHistoryRepository nicknameChangeHistoryRepository;

    private final AuditSignupLogRepository auditSignupLogRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final GmailEmailService gmailEmailService;
    private final UserSecurityService userSecurityService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final EncryptionUtil encryptionUtil;
    private final CertificateService certificateService;
    private final PostService postService;
    private final CommentService commentService;
    private final MessageService messageService;
    private final StellaIconRepository stellaIconRepository;
    private final MessageRepository messageRepository;
    private final PointService pointService;
    private final UserIconRepository userIconRepository;
    private final com.byeolnight.service.auth.EmailAuthService emailAuthService;

    /**
     * 회원가입 처리
     */
    public Long register(UserSignUpRequestDto dto, String ipAddress) {
        try {
            // 이메일 중복 검사 (일반 + 소셜 계정 모두 포함)
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
            // 닉네임 중복 검사
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
            
            // 이메일 인증 확인
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
            

            
            // 기본 소행성 아이콘 부여 및 장착
            grantDefaultAsteroidIcon(savedUser);
            
            // 회원가입 완료 후 인증 상태 삭제
            emailAuthService.clearAllEmailData(dto.getEmail());
            
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
     * 닉네임 중복 검사 (탈퇴/밴 계정 제외)
     */
    public boolean isNicknameDuplicated(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return true; // 빈 닉네임은 중복으로 처리
        }
        
        String trimmedNickname = nickname.trim();
        // 탈퇴/밴 계정의 닉네임은 중복 검사에서 제외
        boolean exists = userRepository.existsByNicknameAndStatusNotIn(
            trimmedNickname, 
            List.of(User.UserStatus.WITHDRAWN, User.UserStatus.BANNED)
        );
        
        log.info("[🔍 닉네임 중복 검사] 입력값: '{}', 정리된 값: '{}', 중복 여부: {} (탈퇴/밴 계정 제외)", nickname, trimmedNickname, exists);
        
        return exists;
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
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        
        return getUserProfile(user.getId());
    }

    /**
     * 사용자 프로필 정보 수정 (닉네임/전화번호)
     */
    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        
        // 소셜 로그인 사용자는 비밀번호 검증 스킵
        if (!user.isSocialUser() && !userSecurityService.matchesPassword(dto.getCurrentPassword(), user.getPassword())) {
            throw new PasswordMismatchException("비밀번호가 일치하지 않습니다.");
        }
        
        // 닉네임이 변경된 경우에만 검증 수행
        if (!user.getNickname().equals(dto.getNickname())) {
            // 닉네임 중복 검사
            if (isNicknameDuplicated(dto.getNickname())) {
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
        
        // 전화번호 변경 기능 제거됨
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
        
        // 소셜 사용자가 아닌 경우에만 비밀번호 검증
        if (!user.isSocialUser() && password != null && !password.isEmpty() && 
            !userSecurityService.matchesPassword(password, user.getPassword())) {
            throw new PasswordMismatchException("비밀번호가 일치하지 않습니다.");
        }
        
        // 소셜 연동 해제 처리 (소셜 사용자인 경우)
        if (user.isSocialUser()) {
            try {
                revokeSocialConnection(user);
            } catch (Exception e) {
                log.warn("소셜 연동 해제 실패 (계속 진행): {}", e.getMessage());
            }
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EmailNotFoundException("존재하지 않는 이메일입니다."));
        
        // 소셜 로그인 사용자는 비밀번호 재설정 불가
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

    /**
     * 비밀번호 검증
     */
    public boolean checkPassword(String rawPassword, User user) {
        // 소셜 로그인 사용자는 비밀번호가 없으므로 항상 false 반환
        if (user.isSocialUser()) {
            return false;
        }
        return userSecurityService.matchesPassword(rawPassword, user.getPassword());
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
        user.changePassword(userSecurityService.encodePassword(newPassword));

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
        
        // 소셜 로그인 사용자는 비밀번호 변경 불가
        if (user.isSocialUser()) {
            throw new IllegalArgumentException("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
        }
        
        // 현재 비밀번호 확인
        if (!userSecurityService.matchesPassword(dto.getCurrentPassword(), user.getPassword())) {
            throw new PasswordMismatchException("현재 비밀번호가 일치하지 않습니다.");
        }
        
        // 새 비밀번호 유효성 검사
        if (!userSecurityService.isValidPassword(dto.getNewPassword())) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이며, 영문/숫자/특수문자를 포함해야 합니다.");
        }
        
        // 비밀번호 변경
        user.changePassword(userSecurityService.encodePassword(dto.getNewPassword()));
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

        // 직접 StellaIcon 조회
        StellaIcon icon = stellaIconRepository.findById(user.getEquippedIconId())
                .orElse(null);
        
        if (icon == null) {
            return null;
        }
        
        return com.byeolnight.dto.shop.EquippedIconDto.builder()
                .iconId(icon.getId())
                .iconName(icon.getIconUrl())
                .iconUrl(icon.getIconUrl())
                .build();
    }



    /**
     * 사용자 프로필 조회 (인증서, 아이콘, 통계 포함)
     */
    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        // 최신 인증서 4개 조회
        List<CertificateDto.Response> certificates = certificateService.getUserPublicCertificates(userId, 4);
        
        // 통계 정보 조회 (블라인드/삭제된 것 제외)
        long postCount = postRepository.countByWriterAndIsDeletedFalse(user);
        long commentCount = commentRepository.countByWriterAndDeletedFalseAndBlindedFalse(user);
        
        // 보유 아이콘 개수 조회
        int totalIconCount = (int) userIconRepository.countByUserId(userId);
        
        // 출석 수 조회 (포인트 히스토리에서 출석체크 타입 개수)
        int attendanceCount;
        try {
            attendanceCount = pointService.getUserAttendanceCount(user);
        } catch (Exception e) {
            // 출석 수 조회 실패 시 가입일부터 계산
            long daysSinceJoined = java.time.temporal.ChronoUnit.DAYS.between(
                user.getCreatedAt().toLocalDate(), 
                LocalDateTime.now().toLocalDate()
            );
            attendanceCount = (int) Math.min(daysSinceJoined, 365); // 최대 365일
        }
        
        // 장착된 아이콘 정보 조회
        com.byeolnight.dto.shop.EquippedIconDto equippedIcon = getUserEquippedIcon(userId);
        
        // 대표 인증서 조회
        List<String> representativeCertificates = new java.util.ArrayList<>();
        try {
            UserCertificate repCert =
                certificateService.getRepresentativeCertificate(user);
            if (repCert != null) {
                representativeCertificates.add(repCert.getCertificateType().getName());
            }
        } catch (Exception e) {
            log.warn("대표 인증서 조회 실패: {}", e.getMessage());
        }
        
        return UserProfileDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .equippedIcon(user.getEquippedIconName())
                .representativeCertificates(representativeCertificates)
                .certificates(certificates)
                .iconCount(totalIconCount)
                .postCount((int) postCount)
                .commentCount((int) commentCount)
                .attendanceCount(attendanceCount)
                .joinedAt(user.getCreatedAt())
                .build();
    }

    /**
     * 기본 소행성 아이콘 부여 및 장착
     */
    @Transactional
    public void grantDefaultAsteroidIcon(User user) {
        try {
            // 소행성 아이콘 조회 (한글명 우선, 영어명 대체)
            StellaIcon asteroidIcon = stellaIconRepository.findByName("소행성")
                    .or(() -> stellaIconRepository.findByName("Asteroid"))
                    .orElse(null);
            
            if (asteroidIcon == null) {
                log.warn("소행성 아이콘을 찾을 수 없습니다. 기본 아이콘 부여를 건너뜁니다.");
                return;
            }
            
            // 이미 소행성 아이콘을 보유하고 있는지 확인
            boolean alreadyOwns = userIconRepository.existsByUserAndStellaIcon(user, asteroidIcon);
            
            if (!alreadyOwns) {
                // 소행성 아이콘 부여 (무료로 지급)
                UserIcon userIcon = UserIcon.builder()
                        .user(user)
                        .stellaIcon(asteroidIcon)
                        .purchasePrice(0) // 기본 아이콘은 무료
                        .build();
                userIconRepository.save(userIcon);
                log.info("사용자 {}에게 기본 소행성 아이콘 부여 완료", user.getNickname());
            }
            
            // 현재 장착된 아이콘이 없으면 소행성 아이콘 장착
            if (user.getEquippedIconId() == null) {
                user.equipIcon(asteroidIcon.getId(), asteroidIcon.getIconUrl());
                userRepository.save(user);
                log.info("사용자 {}에게 기본 소행성 아이콘 장착 완료", user.getNickname());
            }
            
        } catch (Exception e) {
            log.error("기본 소행성 아이콘 부여 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 모든 기존 사용자에게 기본 소행성 아이콘 부여 (마이그레이션용)
     */
    @Transactional
    public void migrateDefaultAsteroidIcon() {
        try {
            List<User> allUsers = userRepository.findAll();
            int processedCount = 0;
            
            for (User user : allUsers) {
                if (user.getStatus() == User.UserStatus.ACTIVE) {
                    grantDefaultAsteroidIcon(user);
                    processedCount++;
                }
            }
            
            log.info("기본 소행성 아이콘 마이그레이션 완료: {}명 처리", processedCount);
        } catch (Exception e) {
            log.error("기본 소행성 아이콘 마이그레이션 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 내 활동 내역 조회 (게시글, 댓글, 쪽지)
     */
    @Transactional(readOnly = true)
    public MyActivityDto getMyActivity(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        System.out.println("내 활동 내역 조회 시작 - userId: " + userId + ", nickname: " + user.getNickname());
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        try {
            // 내가 작성한 게시글
            List<PostDto.Response> myPosts = postService.getMyPosts(userId, pageable);
            System.out.println("내 게시글 조회 완료: " + myPosts.size() + "개");
            
            // 내가 작성한 댓글
            List<CommentDto.Response> myComments = commentService.getMyComments(userId, pageable);
            System.out.println("내 댓글 조회 완료: " + myComments.size() + "개");
            
            // 받은 쪽지
            MessageDto.ListResponse receivedMessages = messageService.getReceivedMessages(userId, pageable);
            System.out.println("받은 쪽지 조회 완료: " + receivedMessages.getMessages().size() + "개");
            
            // 보낸 쪽지
            MessageDto.ListResponse sentMessages = messageService.getSentMessages(userId, pageable);
            System.out.println("보낸 쪽지 조회 완료: " + sentMessages.getMessages().size() + "개");
            
            // 전체 개수 조회
            long totalPostCount = postRepository.countByWriterAndIsDeletedFalse(user);
            long totalCommentCount = commentRepository.countByWriter(user);
            
            System.out.println("전체 게시글 수: " + totalPostCount + ", 전체 댓글 수: " + totalCommentCount);
            
            MyActivityDto result = MyActivityDto.builder()
                    .myPosts(myPosts)
                    .myComments(myComments)
                    .receivedMessages(receivedMessages)
                    .sentMessages(sentMessages)
                    .totalPostCount(totalPostCount)
                    .totalCommentCount(totalCommentCount)
                    .totalReceivedMessageCount(receivedMessages.getTotalCount())
                    .totalSentMessageCount(sentMessages.getTotalCount())
                    .build();
            
            System.out.println("내 활동 내역 조회 완료");
            return result;
            
        } catch (Exception e) {
            System.err.println("내 활동 내역 조회 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 관리자 - 닉네임 변경권 수여
     */
    @Transactional
    public void grantNicknameChangeTicket(Long userId, Long adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("관리자를 찾을 수 없습니다."));
        
        // 닉네임 변경 제한 해제
        user.resetNicknameChangeRestriction();
        
        log.info("관리자 {}가 사용자 {}에게 닉네임 변경권을 수여했습니다.", admin.getNickname(), user.getNickname());
    }

    /**
     * 사용자 저장 (OAuth 사용자 생성용)
     */
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * 닉네임 존재 여부 확인
     */
    public boolean existsByNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    /**
     * 소셜 연동 해제
     */
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

    /**
     * 회원가입 처리 (새로운 메서드)
     */
    @Transactional
    public void createUser(UserSignUpRequestDto dto, jakarta.servlet.http.HttpServletRequest request) {
        String ipAddress = com.byeolnight.infrastructure.util.IpUtil.getClientIp(request);
        register(dto, ipAddress);
    }
}