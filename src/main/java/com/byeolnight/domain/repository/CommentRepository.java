package com.byeolnight.domain.repository;

import com.byeolnight.domain.entity.comment.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 게시글 ID로 댓글 전체 조회 + 작성자까지 즉시 로딩
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.writer WHERE c.post.id = :postId ORDER BY c.createdAt ASC")
    List<Comment> findAllByPostId(@Param("postId") Long postId);

    // 사용자별 댓글 수 조회
    long countByWriter(com.byeolnight.domain.entity.user.User writer);
}
