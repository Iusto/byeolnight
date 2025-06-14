package com.byeolnight.domain.repository;

import com.byeolnight.domain.entity.comment.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 게시글 ID로 댓글 전체 조회 + 작성자까지 즉시 로딩
     * - N+1 문제 방지용 EntityGraph 적용
     */
    @EntityGraph(attributePaths = {"writer"})
    List<Comment> findAllByPostId(Long postId);
}
