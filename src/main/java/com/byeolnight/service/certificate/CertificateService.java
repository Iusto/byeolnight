package com.byeolnight.service.certificate;

import com.byeolnight.domain.entity.certificate.Certificate;
import com.byeolnight.domain.entity.certificate.UserCertificate;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.CommentRepository;
import com.byeolnight.domain.repository.certificate.UserCertificateRepository;
import com.byeolnight.domain.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {

    private final UserCertificateRepository userCertificateRepository;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    // 인증서 발급 체크 및 발급
    @Transactional
    public void checkAndIssueCertificates(User user, CertificateCheckType checkType) {
        switch (checkType) {
            case LOGIN -> checkStarlightExplorer(user);
            case SIGNUP_COMPLETE -> checkSpaceCitizen(user);
            case COMMENT_WRITE -> checkGalaxyCommunicator(user);
            case POST_WRITE -> checkExperimenter(user);
            case IMAGE_VIEW -> checkStarObserver(user);
            case EVENT_VIEW -> checkTourMaster(user);
            case CHAT_PARTICIPATE -> checkChatMaster(user);
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

    // 🚀 은하 통신병 인증서
    private void checkGalaxyCommunicator(User user) {
        if (!hasUserCertificate(user, Certificate.CertificateType.GALAXY_COMMUNICATOR)) {
            long commentCount = commentRepository.countByWriter(user);
            if (commentCount >= 5) {
                issueCertificate(user, Certificate.CertificateType.GALAXY_COMMUNICATOR);
            }
        }
    }

    // 🧪 우주 실험자 인증서
    private void checkExperimenter(User user) {
        if (!hasUserCertificate(user, Certificate.CertificateType.EXPERIMENTER)) {
            long postCount = postRepository.countByWriterAndIsDeletedFalse(user);
            if (postCount >= 3) {
                issueCertificate(user, Certificate.CertificateType.EXPERIMENTER);
            }
        }
    }

    // 🌌 별 관측 매니아 인증서 (IMAGE 게시글 3개 이상 작성)
    private void checkStarObserver(User user) {
        if (!hasUserCertificate(user, Certificate.CertificateType.STAR_OBSERVER)) {
            long imagePostCount = postRepository.countByWriterAndCategoryAndIsDeletedFalse(user, com.byeolnight.domain.entity.post.Post.Category.IMAGE);
            if (imagePostCount >= 3) {
                issueCertificate(user, Certificate.CertificateType.STAR_OBSERVER);
            }
        }
    }

    // 🔭 견학 마스터 인증서 (임시 구현)
    private void checkTourMaster(User user) {
        if (!hasUserCertificate(user, Certificate.CertificateType.TOUR_MASTER)) {
            // 실제로는 스크랩 기능 구현 후 체크
            issueCertificate(user, Certificate.CertificateType.TOUR_MASTER);
        }
    }

    // 💬 별빛 채팅사 인증서 (임시 구현)
    private void checkChatMaster(User user) {
        if (!hasUserCertificate(user, Certificate.CertificateType.CHAT_MASTER)) {
            // 실제로는 채팅 참여 기록을 추적해야 함
            issueCertificate(user, Certificate.CertificateType.CHAT_MASTER);
        }
    }

    // 인증서 발급
    @Transactional
    public void issueCertificate(User user, Certificate.CertificateType certificateType) {
        if (user == null) {
            log.warn("사용자가 null입니다. 인증서 발급을 건너뛁니다.");
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
                .collect(java.util.stream.Collectors.toList());
    }

    // 인증서 정보를 담는 내부 클래스
    public static class CertificateInfo {
        private final Certificate.CertificateType type;
        private final boolean owned;
        private final java.time.LocalDateTime issuedAt;
        private final boolean isRepresentative;

        public CertificateInfo(Certificate.CertificateType type, boolean owned, 
                              java.time.LocalDateTime issuedAt, boolean isRepresentative) {
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
        LOGIN, SIGNUP_COMPLETE, COMMENT_WRITE, POST_WRITE, 
        IMAGE_VIEW, EVENT_VIEW, CHAT_PARTICIPATE
    }
}