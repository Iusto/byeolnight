package com.byeolnight.service.comment;

import com.byeolnight.domain.entity.comment.Comment;
import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.CommentRepository;
import com.byeolnight.domain.repository.post.PostRepository;
import com.byeolnight.dto.comment.CommentRequestDto;
import com.byeolnight.dto.comment.CommentResponseDto;
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
        
        // 댓글 작성 인증서 발급 체크 (완전 비활성화)
        // TODO: 인증서 시스템 구현 완료 후 활성화
        /*
        try {
            certificateService.checkAndIssueCertificates(user, com.byeolnight.service.certificate.CertificateService.CertificateCheckType.COMMENT_WRITE);
        } catch (Exception e) {
            System.err.println("인증서 발급 실패: " + e.getMessage());
        }
        */
        
        // 댓글 작성 포인트 지급
        try {
            pointService.awardCommentWritePoints(user, commentId);
            System.out.println("포인트 지급 완료");
        } catch (Exception e) {
            System.err.println("포인트 지급 실패: " + e.getMessage());
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
        commentRepository.delete(comment);
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

    // 관리자 기능: 댓글 완전 삭제
    @Transactional
    public void deleteCommentPermanently(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글이 존재하지 않습니다."));
        
        // 규정 위반 페널티 적용 (삭제 전에)
        pointService.applyPenalty(comment.getWriter(), "댓글 삭제", commentId.toString());
        
        commentRepository.delete(comment);
    }
}