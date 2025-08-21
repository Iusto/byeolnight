package com.byeolnight.repository.comment;

import com.byeolnight.entity.comment.Comment;
import com.byeolnight.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 게시글 ID로 댓글 전체 조회 + 작성자 즉시 로딩 (LEFT JOIN 사용)
     */
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.writer WHERE c.post.id = :postId ORDER BY c.createdAt ASC")
    List<Comment> findAllByPostId(@Param("postId") Long postId);
    
    /**
     * 게시글 ID로 모든 댓글 조회 (삭제/블라인드 포함) - 관리자용
     */
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.writer WHERE c.post.id = :postId ORDER BY c.createdAt ASC")
    List<Comment> findAllByPostIdIncludingDeleted(@Param("postId") Long postId);
    
    /**
     * 게시글 ID로 인기 댓글 TOP3 조회 (좋아요 5개 이상)
     */
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.writer WHERE c.post.id = :postId AND c.likeCount >= 5 AND c.deleted = false AND c.blinded = false ORDER BY c.likeCount DESC LIMIT 3")
    List<Comment> findTop3PopularCommentsByPostId(@Param("postId") Long postId);
    
    /**
     * 게시글 ID로 댓글 수 조회
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId")
    long countByPostId(@Param("postId") Long postId);

    // 사용자별 댓글 수 조회
    long countByWriter(User writer);
    
    // 사용자별 댓글 수 조회 (블라인드/삭제된 댓글 제외)
    long countByWriterAndDeletedFalseAndBlindedFalse(User writer);
    
    /**
     * 사용자별 작성 댓글 조회
     */
    Page<Comment> findByWriterOrderByCreatedAtDesc(User writer, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 블라인드 처리된 댓글 조회
     */
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.writer LEFT JOIN FETCH c.post WHERE c.blinded = true ORDER BY c.createdAt DESC")
    List<Comment> findByBlindedTrueOrderByCreatedAtDesc();
    
    /**
     * 삭제된 댓글 조회
     */
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.writer LEFT JOIN FETCH c.post WHERE c.deleted = true ORDER BY c.createdAt DESC")
    List<Comment> findByDeletedTrueOrderByCreatedAtDesc();
    
    /**
     * 30일 이상 지난 삭제된 댓글 조회
     */
    @Query("SELECT c FROM Comment c WHERE c.deleted = true AND c.deletedAt < :threshold")
    List<Comment> findExpiredDeletedComments(@Param("threshold") java.time.LocalDateTime threshold);
    
    /**
     * 댓글 내용에 특정 문자열이 포함된 댓글 존재 여부 확인 (S3 파일 사용 여부 체크용)
     */
    boolean existsByContentContaining(String content);
    
    /**
     * 신고 수가 특정 값 이상인 댓글 조회
     */
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.writer LEFT JOIN FETCH c.post WHERE c.reportCount > :count ORDER BY c.reportCount DESC, c.createdAt DESC")
    List<Comment> findByReportCountGreaterThanOrderByReportCountDesc(@Param("count") int count);
}
