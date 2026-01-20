package com.byeolnight.service.certificate;

import com.byeolnight.entity.certificate.Certificate;
import com.byeolnight.entity.certificate.UserCertificate;
import com.byeolnight.entity.user.User;
import com.byeolnight.entity.post.Post;
import com.byeolnight.repository.SuggestionRepository;
import com.byeolnight.repository.chat.ChatParticipationRepository;
import com.byeolnight.repository.comment.CommentRepository;
import com.byeolnight.repository.certificate.UserCertificateRepository;
import com.byeolnight.repository.post.PostReportRepository;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.repository.user.DailyAttendanceRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.dto.certificate.CertificateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {

    private final UserCertificateRepository userCertificateRepository;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ChatParticipationRepository chatParticipationRepository;
    private final PostReportRepository postReportRepository;
    private final SuggestionRepository suggestionRepository;
    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final UserRepository userRepository;

    // 인증서 발급 체크 및 발급
    @Transactional
    public void checkAndIssueCertificates(User user, CertificateCheckType checkType) {
        switch (checkType) {
            case LOGIN -> checkStarlightExplorer(user);
            case POST_WRITE -> {
                checkSpaceCitizen(user);
                checkExperimenter(user);
            }
            case COMMENT_WRITE -> checkGalaxyCommunicator(user);
            case IMAGE_UPLOAD -> checkStarObserver(user);
            case CHAT_PARTICIPATE -> checkChatMaster(user);
            case POINT_ACHIEVEMENT -> checkNightCitizen(user);
            case REPORT_APPROVED -> checkGuardian(user);
            case SUGGESTION_WRITE -> checkSuggestionKing(user);
        }
    }

    // 🌠 별빛 탐험가 인증서
    private void checkStarlightExplorer(User user) {
        if (!hasUserCertificate(user, Certificate.CertificateType.STARLIGHT_EXPLORER)) {
            issueCertificate(user, Certificate.CertificateType.STARLIGHT_EXPLORER);
        }
    }

    // 🪐 우주인 등록 인증서
    private void checkSpaceCitizen(User user) {
        if (!hasUserCertificate(user, Certificate.CertificateType.SPACE_CITIZEN)) {
            issueCertificate(user, Certificate.CertificateType.SPACE_CITIZEN);
        }
    }

    // 🚀 은하 통신병 인증서 (댓글 10회 이상)
    private void checkGalaxyCommunicator(User user) {
        if (!hasUserCertificate(user, Certificate.CertificateType.GALAXY_COMMUNICATOR)) {
            long commentCount = commentRepository.countByWriter(user);
            if (commentCount >= 10) {
                issueCertificate(user, Certificate.CertificateType.GALAXY_COMMUNICATOR);
            }
        }
    }

    // 🧪 우주 실험자 인증서 (게시글 5개 이상 + 서로 다른 게시판 3곳 이상)
    private void checkExperimenter(User user) {
        if (!hasUserCertificate(user, Certificate.CertificateType.EXPERIMENTER)) {
            long postCount = postRepository.countByWriterAndIsDeletedFalse(user);
            long categoryCount = postRepository.countDistinctCategoriesByWriter(user);
            if (postCount >= 5 && categoryCount >= 3) {
                issueCertificate(user, Certificate.CertificateType.EXPERIMENTER);
            }
        }
    }

    // 🌌 별 관측 매니아 인증서 (IMAGE 게시판에 사진 5장 이상 업로드)
    private void checkStarObserver(User user) {
        if (!hasUserCertificate(user, Certificate.CertificateType.STAR_OBSERVER)) {
            long imagePostCount = postRepository.countByWriterAndCategoryAndIsDeletedFalse(user, Post.Category.IMAGE);
            if (imagePostCount >= 5) {
                issueCertificate(user, Certificate.CertificateType.STAR_OBSERVER);
            }
        }
    }

    // 🌟 별 헤는 밤 시민증 (누적 스텔라포인트 350점 이상)
    private void checkNightCitizen(User user) {
        if (!hasUserCertificate(user, Certificate.CertificateType.NIGHT_CITIZEN)) {
            if (user.getPoints() >= 350) {
                issueCertificate(user, Certificate.CertificateType.NIGHT_CITIZEN);
            }
        }
    }

    // 💬 별빛 채팅사 인증서 (출석일수 3회 + 채팅 10회)
    private void checkChatMaster(User user) {
        if (!hasUserCertificate(user, Certificate.CertificateType.CHAT_MASTER)) {
            Long totalMessages = chatParticipationRepository.getTotalMessageCountByUser(user);
            long attendanceCount = dailyAttendanceRepository.countByUser(user);
            
            if (totalMessages != null && totalMessages >= 10 && attendanceCount >= 3) {
                issueCertificate(user, Certificate.CertificateType.CHAT_MASTER);
            }
        }
    }

    // 🛰️ 별빛 수호자 인증서 (게시글 신고 5회 이상 + 3건 이상 관리자 승인)
    private void checkGuardian(User user) {
        if (!hasUserCertificate(user, Certificate.CertificateType.GUARDIAN)) {
            long totalReports = postReportRepository.countByUser(user);
            long approvedReports = postReportRepository.countApprovedReportsByUser(user);
            
            if (totalReports >= 5 && approvedReports >= 3) {
                issueCertificate(user, Certificate.CertificateType.GUARDIAN);
            }
        }
    }

    // 💡 건의왕 인증서 (건의사항 3건 이상 작성)
    private void checkSuggestionKing(User user) {
        if (!hasUserCertificate(user, Certificate.CertificateType.SUGGESTION_KING)) {
            long suggestionCount = suggestionRepository.countByAuthor(user);
            
            if (suggestionCount >= 3) {
                issueCertificate(user, Certificate.CertificateType.SUGGESTION_KING);
            }
        }
    }

    // 인증서 발급
    @Transactional
    public void issueCertificate(User user, Certificate.CertificateType certificateType) {
        if (user == null) {
            log.warn("사용자가 null입니다. 인증서 발급을 건너뜁니다.");
            return;
        }
        
        if (!hasUserCertificate(user, certificateType)) {
            UserCertificate userCertificate = UserCertificate.of(user, certificateType);
            userCertificateRepository.save(userCertificate);
            log.info("인증서 발급: {} - {}", user.getNickname(), certificateType.getName());
        }
    }

    // 대표 인증서 설정
    @Transactional
    public void setRepresentativeCertificate(User user, Certificate.CertificateType certificateType) {
        // 기존 대표 인증서 해제
        userCertificateRepository.unsetAllRepresentativeCertificates(user);
        
        // 새 대표 인증서 설정
        userCertificateRepository.findByUserAndCertificateType(user, certificateType)
                .ifPresent(UserCertificate::setAsRepresentative);
    }

    // 사용자 인증서 목록 조회
    public List<UserCertificate> getUserCertificates(User user) {
        return userCertificateRepository.findByUserOrderByCreatedAtDesc(user);
    }

    // 대표 인증서 조회
    public UserCertificate getRepresentativeCertificate(User user) {
        return userCertificateRepository.findByUserAndIsRepresentativeTrue(user).orElse(null);
    }

    /**
     * 여러 사용자의 대표 인증서를 한 번에 조회 (N+1 방지용 배치 조회)
     * @param users 조회할 사용자 Set
     * @return userId -> UserCertificate 매핑
     */
    public java.util.Map<Long, UserCertificate> getRepresentativeCertificatesForUsers(Set<User> users) {
        if (users == null || users.isEmpty()) {
            return java.util.Collections.emptyMap();
        }

        List<UserCertificate> certificates = userCertificateRepository.findByUserInAndIsRepresentativeTrue(users);

        return certificates.stream()
                .collect(Collectors.toMap(
                        cert -> cert.getUser().getId(),
                        cert -> cert,
                        (existing, replacement) -> existing // 중복 시 기존 값 유지
                ));
    }

    /**
     * 대표 인증서 안전 조회 (예외 발생 시 null 반환)
     */
    public UserCertificate getRepresentativeCertificateSafe(User user) {
        try {
            return getRepresentativeCertificate(user);
        } catch (Exception e) {
            return null;
        }
    }

    // 인증서 보유 여부 확인
    private boolean hasUserCertificate(User user, Certificate.CertificateType certificateType) {
        if (user == null) {
            return false;
        }
        return userCertificateRepository.existsByUserAndCertificateType(user, certificateType);
    }

    // 전체 인증서 목록 조회 (보유/미보유 구분)
    public List<CertificateInfo> getAllCertificatesWithStatus(User user) {
        List<UserCertificate> userCertificates = userCertificateRepository.findByUserOrderByCreatedAtDesc(user);
        Set<Certificate.CertificateType> ownedTypes = userCertificates.stream()
                .map(UserCertificate::getCertificateType)
                .collect(java.util.stream.Collectors.toSet());

        return java.util.Arrays.stream(Certificate.CertificateType.values())
                .map(type -> new CertificateInfo(
                    type,
                    ownedTypes.contains(type),
                    userCertificates.stream()
                            .filter(uc -> uc.getCertificateType() == type)
                            .findFirst()
                            .map(UserCertificate::getCreatedAt)
                            .orElse(null),
                    userCertificates.stream()
                            .anyMatch(uc -> uc.getCertificateType() == type && uc.isRepresentative())
                ))
                .collect(Collectors.toList());
    }

    // 인증서 정보를 담는 내부 클래스
    public static class CertificateInfo {
        private final Certificate.CertificateType type;
        private final boolean owned;
        private final LocalDateTime issuedAt;
        private final boolean isRepresentative;

        public CertificateInfo(Certificate.CertificateType type, boolean owned,
                               LocalDateTime issuedAt, boolean isRepresentative) {
            this.type = type;
            this.owned = owned;
            this.issuedAt = issuedAt;
            this.isRepresentative = isRepresentative;
        }

        public Certificate.CertificateType getType() { return type; }
        public boolean isOwned() { return owned; }
        public java.time.LocalDateTime getIssuedAt() { return issuedAt; }
        public boolean isRepresentative() { return isRepresentative; }
    }

    public enum CertificateCheckType {
        LOGIN, POST_WRITE, COMMENT_WRITE, IMAGE_UPLOAD, 
        CHAT_PARTICIPATE, POINT_ACHIEVEMENT, REPORT_APPROVED, SUGGESTION_WRITE
    }

    /**
     * 사용자의 공개 인증서 조회 (최신순 제한)
     */
    public List<CertificateDto.Response> getUserPublicCertificates(Long userId, int limit) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            
            if (user == null) {
                return Collections.emptyList();
            }
            
            List<UserCertificate> userCertificates = userCertificateRepository
                .findByUserOrderByCreatedAtDesc(user)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
            
            return userCertificates.stream()
                .map(uc -> CertificateDto.Response.builder()
                    .id(uc.getId())
                    .title(uc.getCertificateType().getName())
                    .description(uc.getCertificateType().getDescription())
                    .iconUrl(uc.getCertificateType().getIcon())
                    .earnedAt(uc.getCreatedAt())
                    .build())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("인증서 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}