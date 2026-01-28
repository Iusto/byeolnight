package com.byeolnight.service.user;

import com.byeolnight.dto.certificate.CertificateDto;
import com.byeolnight.dto.comment.CommentDto;
import com.byeolnight.dto.message.MessageDto;
import com.byeolnight.dto.post.PostDto;
import com.byeolnight.dto.user.MyActivityDto;
import com.byeolnight.dto.user.UpdateProfileRequestDto;
import com.byeolnight.dto.user.UserProfileDto;
import com.byeolnight.entity.certificate.UserCertificate;
import com.byeolnight.entity.shop.StellaIcon;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.exception.NotFoundException;
import com.byeolnight.infrastructure.exception.PasswordMismatchException;
import com.byeolnight.repository.comment.CommentRepository;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.repository.shop.StellaIconRepository;
import com.byeolnight.repository.shop.UserIconRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.service.certificate.CertificateService;
import com.byeolnight.service.comment.CommentService;
import com.byeolnight.service.message.MessageService;
import com.byeolnight.service.post.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 프로필 조회/수정 서비스
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final StellaIconRepository stellaIconRepository;
    private final UserIconRepository userIconRepository;
    private final CertificateService certificateService;
    private final PostService postService;
    private final CommentService commentService;
    private final MessageService messageService;
    private final PointService pointService;
    private final UserSecurityService userSecurityService;
    private final UserQueryService userQueryService;

    public UserProfileDto getUserProfileByNickname(String nickname) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        return getUserProfile(user.getId());
    }

    public UserProfileDto getUserProfile(Long userId) {
        User user = userQueryService.findById(userId);

        List<CertificateDto.Response> certificates = certificateService.getUserPublicCertificates(userId, 4);
        long postCount = postRepository.countByWriterAndIsDeletedFalse(user);
        long commentCount = commentRepository.countByWriterAndDeletedFalseAndBlindedFalse(user);
        int totalIconCount = (int) userIconRepository.countByUserId(userId);
        
        int attendanceCount;
        try {
            attendanceCount = pointService.getUserAttendanceCount(user);
        } catch (Exception e) {
            long daysSinceJoined = java.time.temporal.ChronoUnit.DAYS.between(
                user.getCreatedAt().toLocalDate(), 
                LocalDateTime.now().toLocalDate()
            );
            attendanceCount = (int) Math.min(daysSinceJoined, 365);
        }
        
        com.byeolnight.dto.shop.EquippedIconDto equippedIcon = getUserEquippedIcon(userId);
        
        List<String> representativeCertificates = new ArrayList<>();
        try {
            UserCertificate repCert = certificateService.getRepresentativeCertificate(user);
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

    public com.byeolnight.dto.shop.EquippedIconDto getUserEquippedIcon(Long userId) {
        User user = userQueryService.findById(userId);

        if (user.getEquippedIconId() == null) {
            return null;
        }

        StellaIcon icon = stellaIconRepository.findById(user.getEquippedIconId()).orElse(null);
        if (icon == null) {
            return null;
        }
        
        return com.byeolnight.dto.shop.EquippedIconDto.builder()
                .iconId(icon.getId())
                .iconName(icon.getIconUrl())
                .iconUrl(icon.getIconUrl())
                .build();
    }

    public MyActivityDto getMyActivity(Long userId, int page, int size) {
        User user = userQueryService.findById(userId);
        log.debug("내 활동 내역 조회 시작 - userId: {}, nickname: {}", userId, user.getNickname());

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        try {
            org.springframework.data.domain.Page<PostDto.Response> postsPage = postService.getMyPosts(userId, pageable);
            log.debug("내 게시글 조회 완료: {}개", postsPage.getContent().size());

            org.springframework.data.domain.Page<CommentDto.Response> commentsPage = commentService.getMyComments(userId, pageable);
            log.debug("내 댓글 조회 완료: {}개", commentsPage.getContent().size());

            MessageDto.ListResponse receivedMessages = messageService.getReceivedMessages(userId, pageable);
            log.debug("받은 쪽지 조회 완료: {}개", receivedMessages.getMessages().size());

            MessageDto.ListResponse sentMessages = messageService.getSentMessages(userId, pageable);
            log.debug("보낸 쪽지 조회 완료: {}개", sentMessages.getMessages().size());

            long totalPostCount = postRepository.countByWriterAndIsDeletedFalse(user);
            long totalCommentCount = commentRepository.countByWriter(user);

            log.debug("전체 게시글 수: {}, 전체 댓글 수: {}", totalPostCount, totalCommentCount);

            MyActivityDto result = MyActivityDto.builder()
                    .myPosts(postsPage.getContent())
                    .myComments(commentsPage.getContent())
                    .receivedMessages(receivedMessages)
                    .sentMessages(sentMessages)
                    .totalPostCount(totalPostCount)
                    .totalCommentCount(totalCommentCount)
                    .totalReceivedMessageCount(receivedMessages.getTotalCount())
                    .totalSentMessageCount(sentMessages.getTotalCount())
                    // 게시글 페이징 정보
                    .postsCurrentPage(postsPage.getNumber())
                    .postsTotalPages(postsPage.getTotalPages())
                    .postsHasNext(postsPage.hasNext())
                    .postsHasPrevious(postsPage.hasPrevious())
                    // 댓글 페이징 정보
                    .commentsCurrentPage(commentsPage.getNumber())
                    .commentsTotalPages(commentsPage.getTotalPages())
                    .commentsHasNext(commentsPage.hasNext())
                    .commentsHasPrevious(commentsPage.hasPrevious())
                    .build();

            log.debug("내 활동 내역 조회 완료");
            return result;

        } catch (Exception e) {
            log.error("내 활동 내역 조회 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequestDto dto) {
        User user = userQueryService.findById(userId);
        
        if (!user.isSocialUser() && !userSecurityService.matchesPassword(dto.getCurrentPassword(), user.getPassword())) {
            throw new PasswordMismatchException("비밀번호가 일치하지 않습니다.");
        }
        
        if (!user.getNickname().equals(dto.getNickname())) {
            if (userRepository.existsByNicknameAndStatusNotIn(
                dto.getNickname().trim(), 
                List.of(User.UserStatus.WITHDRAWN, User.UserStatus.BANNED)
            )) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            }
            
            if (user.isNicknameChanged() && user.getNicknameUpdatedAt() != null &&
                    user.getNicknameUpdatedAt().isAfter(LocalDateTime.now().minusMonths(6))) {
                throw new IllegalArgumentException("닉네임은 6개월마다 변경할 수 있습니다. 다음 변경 가능 시기: " + 
                        user.getNicknameUpdatedAt().plusMonths(6).toLocalDate());
            }
            
            user.updateNickname(dto.getNickname(), LocalDateTime.now());
        }
    }
}
