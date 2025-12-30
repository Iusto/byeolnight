package com.byeolnight.service.comment;

import com.byeolnight.entity.Notification;
import com.byeolnight.entity.comment.Comment;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.entity.comment.CommentLike;
import com.byeolnight.repository.comment.CommentLikeRepository;
import com.byeolnight.repository.comment.CommentRepository;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.dto.comment.CommentRequestDto;
import com.byeolnight.dto.comment.CommentResponseDto;
import com.byeolnight.dto.comment.CommentDto;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.service.certificate.CertificateService;
import com.byeolnight.service.notification.NotificationService;
import com.byeolnight.service.user.PointService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.byeolnight.infrastructure.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final CertificateService certificateService;
    private final PointService pointService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final CommentLikeRepository commentLikeRepository;

    @Transactional
    public Long create(CommentRequestDto dto, User user) {
        if (user == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        
        Post post = postRepository.findById(dto.getPostId())
                .orElseThrow(() -> new NotFoundException("게시글이 존재하지 않습니다."));

        // 답글인 경우 부모 댓글 확인
        Comment parentComment = null;
        if (dto.getParentId() != null) {
            parentComment = commentRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new NotFoundException("부모 댓글이 존재하지 않습니다."));
        }

        Comment comment = Comment.builder()
                .post(post)
                .writer(user)
                .content(dto.getContent())
                .parent(parentComment)
                .build();

        Comment savedComment = commentRepository.save(comment);
        Long commentId = savedComment.getId();
        
        // 댓글 작성 인증서 발급 체크
        try {
            certificateService.checkAndIssueCertificates(user, CertificateService.CertificateCheckType.COMMENT_WRITE);
        } catch (Exception e) {
            // 인증서 발급 실패 무시 - 주요 기능 아님
        }
        
        // 댓글 작성 포인트 지급
        try {
            pointService.awardCommentWritePoints(user, commentId);
        } catch (Exception e) {
            // 포인트 지급 실패 무시 - 주요 기능 아님
        }
        
        // 알림 생성
        try {
            if (parentComment != null) {
                // 답글인 경우 - 부모 댓글 작성자에게 알림
                if (!parentComment.getWriter().getId().equals(user.getId())) {
                    notificationService.createNotification(
                        parentComment.getWriter().getId(),
                        Notification.NotificationType.REPLY_ON_COMMENT,
                        "댓글에 답글이 달렸습니다",
                        user.getNickname() + "님이 회원님의 댓글에 답글을 달았습니다.",
                        "/posts/" + post.getId(),
                        commentId
                    );
                }
            } else {
                // 일반 댓글인 경우 - 게시글 작성자에게 알림
                if (!post.getWriter().getId().equals(user.getId())) {
                    notificationService.createNotification(
                        post.getWriter().getId(),
                        Notification.NotificationType.COMMENT_ON_POST,
                        "게시글에 댓글이 달렸습니다",
                        user.getNickname() + "님이 회원님의 게시글에 댓글을 달았습니다.",
                        "/posts/" + post.getId(),
                        commentId
                    );
                }
            }
        } catch (Exception e) {
            // 알림 생성 실패 무시 - 주요 기능 아님
        }
        
        return commentId;
    }

    public List<CommentResponseDto> getByPostId(Long postId, User currentUser) {
        if (postId == null || postId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 게시글 ID입니다.");
        }
        
        // 게시글 존재 여부 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글이 존재하지 않습니다."));
        
        List<Comment> comments = commentRepository.findAllByPostId(postId);
        
        return comments.stream()
                .map(comment -> CommentResponseDto.from(comment, currentUser))
                .collect(Collectors.toList());
    }

    @Transactional
    public void update(Long commentId, CommentRequestDto dto, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글이 존재하지 않습니다."));

        if (!comment.getWriter().equals(user)) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }
        comment.update(dto.getContent());
    }

    @Transactional
    public void delete(Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글이 존재하지 않습니다."));

        if (!comment.getWriter().equals(user)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }
        comment.softDelete(); // soft delete로 변경
    }

    /**
     * 내가 작성한 댓글 조회
     */
    @Transactional(readOnly = true)
    public List<CommentDto.Response> getMyComments(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        
        Page<Comment> comments = commentRepository.findByWriterOrderByCreatedAtDesc(user, pageable);
        
        return comments.getContent().stream()
                .map(CommentDto.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getBlindedComments(User currentUser) {
        return commentRepository.findByBlindedTrueOrderByCreatedAtDesc().stream()
                .map(comment -> CommentResponseDto.from(comment, currentUser))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getDeletedComments(User currentUser) {
        return commentRepository.findByDeletedTrueOrderByCreatedAtDesc().stream()
                .map(comment -> CommentResponseDto.from(comment, currentUser))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getPostCommentsForAdmin(Long postId, User currentUser) {
        if (postId == null || postId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 게시글 ID입니다.");
        }
        
        // 게시글 존재 여부 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글이 존재하지 않습니다."));
        
        // 관리자는 삭제된 댓글과 블라인드된 댓글도 모두 조회 가능
        List<Comment> comments = commentRepository.findAllByPostIdIncludingDeleted(postId);
        
        return comments.stream()
                .map(comment -> CommentResponseDto.fromForAdmin(comment, currentUser))
                .collect(Collectors.toList());
    }
    
    // 댓글 좋아요/취소 (DB 유니크 제약조건 활용)
    @Transactional
    public boolean toggleCommentLike(Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글이 존재하지 않습니다."));

        boolean exists = commentLikeRepository.existsByCommentAndUser(comment, user);

        if (exists) {
            // 좋아요 취소
            commentLikeRepository.deleteByCommentAndUser(comment, user);
            comment.decreaseLikeCount();
            return false;
        } else {
            // 좋아요 추가 - DB 유니크 제약조건(comment_id, user_id)이 중복 방지
            try {
                CommentLike like = CommentLike.builder()
                        .comment(comment)
                        .user(user)
                        .build();
                commentLikeRepository.save(like);
                comment.increaseLikeCount();
                return true;
            } catch (DataIntegrityViolationException e) {
                // 중복 좋아요 시도 - 이미 존재하는 것으로 간주
                log.debug("중복 좋아요 시도: userId={}, commentId={}", user.getId(), commentId);
                return true; // 이미 좋아요한 상태
            }
        }
    }

    // 관리자 기능: 댓글 블라인드 처리
    @Transactional
    public void blindComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글이 존재하지 않습니다."));
        comment.blind();
        
        // 규정 위반 페널티 적용
        pointService.applyPenalty(comment.getWriter(), "댓글 블라인드 처리", commentId.toString());
    }

    // 관리자 기능: 댓글 블라인드 해제
    @Transactional
    public void unblindComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글이 존재하지 않습니다."));
        comment.unblind();
    }
}