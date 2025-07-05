package com.byeolnight.domain.repository;

import com.byeolnight.domain.entity.comment.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
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
     * 게시글 ID로 댓글 전체 조회 (간단한 방식)
     */
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId ORDER BY c.createdAt ASC")
    List<Comment> findAllByPostIdSimple(@Param("postId") Long postId);
    
    /**
     * 게시글 ID로 댓글 수 조회
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId")
    long countByPostId(@Param("postId") Long postId);
    


    // 사용자별 댓글 수 조회
    long countByWriter(com.byeolnight.domain.entity.user.User writer);
    
    // 작성자 ID로 댓글 수 조회 - 제거됨 (writer 필드 사용)
    
    // 사용자가 받은 댓글 좋아요 수 조회 (현재 좋아요 기능 미구현으로 0 반환)
    @Query("SELECT 0")
    long countLikesByUser(@Param("userId") Long userId);
    
    /**
     * 사용자별 작성 댓글 조회
     */
    org.springframework.data.domain.Page<Comment> findByWriterOrderByCreatedAtDesc(com.byeolnight.domain.entity.user.User writer, org.springframework.data.domain.Pageable pageable);
}
