package com.byeolnight.domain.repository.post;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.post.Post.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // 🔹 전체 게시글 목록 조회 (isDeleted = false 조건)
    Page<Post> findAllByIsDeletedFalse(Pageable pageable);

    // 🔹 카테고리별 게시글 목록 조회 (최신순, 삭제되지 않은 게시글만)
    Page<Post> findByIsDeletedFalseAndCategoryOrderByCreatedAtDesc(Category category, Pageable pageable);

    // 🔹 전체 게시글 목록 조회 (최신순, 삭제되지 않은 게시글만)
    Page<Post> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    // 🔹 다건 조회용: 추천순 정렬 등 ID 리스트로 여러 게시글을 fetch join으로 조회할 때 사용
    // - writer 정보를 lazy loading 없이 한 번에 가져옴 (N+1 문제 해결 목적)
    @Query("""
        SELECT DISTINCT p FROM Post p
        JOIN FETCH p.writer
        WHERE p.id IN :ids AND p.isDeleted = false
    """)
    List<Post> findPostsByIds(@Param("ids") List<Long> ids);

    // 🔹 단건 조회용: 게시글 상세 조회 시 writer 정보까지 즉시 로딩(fetch join)하여 반환
    // - Hibernate.initialize() 대신 fetch join으로 lazy 예외 방지
    @Query("""
        SELECT p FROM Post p
        JOIN FETCH p.writer
        WHERE p.id = :id AND p.isDeleted = false
    """)
    Optional<Post> findWithWriterById(@Param("id") Long id);
}
