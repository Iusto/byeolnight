package com.byeolnight.domain.repository;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.post.Post.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByCategory(Category category);

    List<Post> findAllByOrderByCreatedAtDesc();

    Page<Post> findAllByIsDeletedFalse(Pageable pageable);

    Page<Post> findByIsDeletedFalseAndCategoryOrderByCreatedAtDesc(Category category, Pageable pageable);

    Page<Post> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    // ✅ 좋아요 순 정렬 (카테고리 조건 포함)
    @Query("""
        SELECT p FROM Post p
        LEFT JOIN PostLike pl ON pl.post = p
        WHERE p.isDeleted = false
        AND (:category IS NULL OR p.category = :category)
        GROUP BY p
        ORDER BY COUNT(pl.id) DESC
    """)
    Page<Post> findPostsByCategoryOrderByLikeCountDesc(
            @Param("category") Category category,
            Pageable pageable
    );
}
