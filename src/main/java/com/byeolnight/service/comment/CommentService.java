package com.byeolnight.service.comment;

import com.byeolnight.domain.entity.comment.Comment;
import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.comment.CommentRepository;
import com.byeolnight.domain.repository.post.PostRepository;
import com.byeolnight.dto.comment.CommentRequestDto;
import com.byeolnight.dto.comment.CommentResponseDto;
import com.byeolnight.dto.comment.CommentDto;
import com.byeolnight.domain.repository.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.byeolnight.infrastructure.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final com.byeolnight.service.certificate.CertificateService certificateService;
    private final com.byeolnight.service.user.PointService pointService;
    private final UserRepository userRepository;
    private final com.byeolnight.service.notification.NotificationService notificationService;
    private final com.byeolnight.domain.repository.comment.CommentLikeRepository commentLikeRepository;
    private final com.byeolnight.domain.repository.comment.CommentReportRepository commentReportRepository;

    @Transactional
    public Long create(CommentRequestDto dto, User user) {
        System.out.println("=== 댓글 등록 시작 ===");
        System.out.println("postId: " + dto.getPostId());
        System.out.println("content: " + dto.getContent());
        System.out.println("user: " + user);
        if (user != null) {
            System.out.println("user ID: " + user.getId());
            System.out.println("user nickname: " + user.getNickname());
        } else {
            System.err.println("사용자가 null입니다!");
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        Post post = postRepository.findById(dto.getPostId())
                .orElseThrow(() -> new NotFoundException("게시글이 존재하지 않습니다."));

        // 답글인 경우 부모 댓글 확인
        Comment parentComment = null;
        if (dto.getParentId() != null) {
            parentComment = commentRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new NotFoundException("부모 댓글이 존재하지 않습니다."));
            System.out.println("답글 등록 - 부모 댓글 ID: " + dto.getParentId());
        }

        Comment comment = Comment.builder()
                .post(post)
                .writer(user)
                .content(dto.getContent())
                .parent(parentComment)
                .build();

        Comment savedComment = commentRepository.save(comment);
        Long commentId = savedComment.getId();
        
        // 저장된 댓글 확인
        System.out.println("댓글 저장 완료 - ID: " + commentId);
        System.out.println("입력된 user: " + (user != null ? user.getNickname() : "null"));
        System.out.println("저장된 댓글 writer: " + (savedComment.getWriter() != null ? savedComment.getWriter().getNickname() : "null"));
        
        // 다시 조회해서 확인
        Comment reloadedComment = commentRepository.findById(commentId).orElse(null);
        if (reloadedComment != null) {
            System.out.println("다시 조회한 댓글 writer: " + (reloadedComment.getWriter() != null ? reloadedComment.getWriter().getNickname() : "null"));
        }
        
        // 댓글 작성 인증서 발급 체크
        try {
            certificateService.checkAndIssueCertificates(user, com.byeolnight.service.certificate.CertificateService.CertificateCheckType.COMMENT_WRITE);
        } catch (Exception e) {
            System.err.println("인증서 발급 실패: " + e.getMessage());
        }
        
        // 댓글 작성 포인트 지급
        try {
            pointService.awardCommentWritePoints(user, commentId);
            System.out.println("포인트 지급 완료");
        } catch (Exception e) {
            System.err.println("포인트 지급 실패: " + e.getMessage());
        }
        
        // 알림 생성
        try {
            if (parentComment != null) {
                // 답글인 경우 - 부모 댓글 작성자에게 알림
                if (!parentComment.getWriter().getId().equals(user.getId())) {
                    notificationService.createNotification(
                        parentComment.getWriter().getId(),
                        com.byeolnight.domain.entity.Notification.NotificationType.REPLY_ON_COMMENT,
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
                        com.byeolnight.domain.entity.Notification.NotificationType.COMMENT_ON_POST,
                        "게시글에 댓글이 달렸습니다",
                        user.getNickname() + "님이 회원님의 게시글에 댓글을 달았습니다.",
                        "/posts/" + post.getId(),
                        commentId
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("알림 생성 실패: " + e.getMessage());
        }
        
        System.out.println("댓글 등록 완전 완료 - commentId: " + commentId);
        return commentId;
    }

    public List<CommentResponseDto> getByPostId(Long postId) {
        System.out.println("=== 댓글 조회 API 호출 시작 ===");
        System.out.println("요청된 postId: " + postId);
        
        if (postId == null || postId <= 0) {
            System.out.println("잘못된 postId: " + postId);
            throw new IllegalArgumentException("유효하지 않은 게시글 ID입니다.");
        }
        
        // 게시글 존재 여부 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글이 존재하지 않습니다."));
        System.out.println("게시글 확인 완료 - postId: " + postId + ", title: " + post.getTitle());
        
        // 데이터베이스에서 직접 댓글 수 확인
        long totalCommentCount = commentRepository.count();
        long postCommentCount = commentRepository.countByPostId(postId);
        System.out.println("전체 댓글 수: " + totalCommentCount);
        System.out.println("postId " + postId + "의 댓글 수: " + postCommentCount);
        
        List<Comment> comments = commentRepository.findAllByPostId(postId);
        System.out.println("조회된 댓글 수: " + comments.size());
        
        // 각 댓글 정보 상세 출력
        for (Comment comment : comments) {
            System.out.println("=== 댓글 상세 정보 ===");
            System.out.println("댓글 ID: " + comment.getId());
            System.out.println("내용: " + comment.getContent());
            System.out.println("Writer 객체: " + comment.getWriter());
            if (comment.getWriter() != null) {
                System.out.println("Writer ID: " + comment.getWriter().getId());
                System.out.println("Writer 닉네임: " + comment.getWriter().getNickname());
            } else {
                System.out.println("Writer가 null입니다!");
            }
            System.out.println("========================");
        }
        
        // 임시로 모든 댓글 반환 (필터링 비활성화)
        System.out.println("전체 댓글 수: " + comments.size());
        
        List<CommentResponseDto> result = comments.stream()
                .map(CommentResponseDto::from)
                .collect(Collectors.toList());
        
        System.out.println("최종 반환 댓글 수: " + result.size());
        System.out.println("=== 댓글 조회 API 완료 ===");
        
        return result;
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

    /**
     * 내가 작성한 댓글 조회
     */
    @Transactional(readOnly = true)
    public List<CommentDto.Response> getMyComments(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        
        System.out.println("내 댓글 조회 - userId: " + userId + ", nickname: " + user.getNickname());
        
        Page<Comment> comments = commentRepository.findByWriterOrderByCreatedAtDesc(user, pageable);
        System.out.println("조회된 댓글 수: " + comments.getContent().size());
        
        return comments.getContent().stream()
                .map(comment -> {
                    System.out.println("댓글 변환: " + comment.getContent().substring(0, Math.min(20, comment.getContent().length())) + "...");
                    return CommentDto.Response.from(comment);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getBlindedComments() {
        return commentRepository.findByBlindedTrueOrderByCreatedAtDesc().stream()
                .map(CommentResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getDeletedComments() {
        return commentRepository.findByDeletedTrueOrderByCreatedAtDesc().stream()
                .map(CommentResponseDto::from)
                .toList();
    }
    
    // 댓글 좋아요/취소
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
            // 좋아요 추가
            com.byeolnight.domain.entity.comment.CommentLike like = 
                com.byeolnight.domain.entity.comment.CommentLike.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            commentLikeRepository.save(like);
            comment.increaseLikeCount();
            return true;
        }
    }
    
    // 댓글 신고
    @Transactional
    public void reportComment(Long commentId, User reporter, String reason, String description) {
        System.out.println("=== 댓글 신고 시작 ===");
        System.out.println("commentId: " + commentId);
        System.out.println("reporter: " + (reporter != null ? reporter.getId() + ", " + reporter.getNickname() : "null"));
        System.out.println("reason: " + reason);
        
        if (reporter == null) {
            System.err.println("신고자가 null입니다!");
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        
        // 신고자 새로 조회
        User freshReporter = userRepository.findById(reporter.getId())
                .orElseThrow(() -> {
                    System.err.println("신고자 ID가 데이터베이스에 존재하지 않습니다: " + reporter.getId());
                    return new NotFoundException("신고자 정보가 유효하지 않습니다.");
                });
        
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글이 존재하지 않습니다."));
        
        // 중복 신고 방지
        if (commentReportRepository.existsByCommentAndReporter(comment, freshReporter)) {
            throw new IllegalArgumentException("이미 신고한 댓글입니다.");
        }
        
        try {
            // 신고 객체 생성 전 데이터 출력
            System.out.println("신고 객체 생성 전 데이터 확인:");
            System.out.println("- 댓글 ID: " + comment.getId());
            System.out.println("- 신고자 ID: " + freshReporter.getId());
            System.out.println("- 신고자 닉네임: " + freshReporter.getNickname());
            System.out.println("- 신고 사유: " + reason);
            
            // 신고 객체 생성 - 생성자 사용
            com.byeolnight.domain.entity.comment.CommentReport report = new com.byeolnight.domain.entity.comment.CommentReport();
            report.setComment(comment);
            report.setReporter(freshReporter);
            report.setReason(reason);
            report.setDescription(description);
            report.setStatus(com.byeolnight.domain.entity.comment.CommentReport.ReportStatus.PENDING);
            report.setCreatedAt(java.time.LocalDateTime.now());
            
            // 저장 전 객체 확인
            System.out.println("저장 전 신고 객체 확인:");
            System.out.println("- 댓글: " + report.getComment().getId());
            System.out.println("- 신고자: " + report.getReporter().getId());
            
            commentReportRepository.save(report);
            comment.increaseReportCount();
            
            // 신고 수가 5개 이상이면 자동 블라인드
            if (comment.getReportCount() >= 5) {
                comment.blind();
            }
            
            System.out.println("댓글 신고 성공 - reportId: " + report.getId());
        } catch (Exception e) {
            System.err.println("댓글 신고 오류: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    // 관리자: 댓글 신고 처리
    @Transactional
    public void processCommentReport(Long reportId, User admin, boolean approve) {
        com.byeolnight.domain.entity.comment.CommentReport report = 
            commentReportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("신고를 찾을 수 없습니다."));
        
        if (approve) {
            report.approve(admin);
            report.getComment().blind();
            // 페널티 적용
            pointService.applyPenalty(report.getComment().getWriter(), "댓글 신고 승인", reportId.toString());
        } else {
            report.reject(admin);
            report.getComment().decreaseReportCount();
        }
    }
    
    // 관리자: 대기 중인 댓글 신고 목록
    @Transactional(readOnly = true)
    public List<com.byeolnight.domain.entity.comment.CommentReport> getPendingCommentReports() {
        return commentReportRepository.findPendingReports();
    }
}