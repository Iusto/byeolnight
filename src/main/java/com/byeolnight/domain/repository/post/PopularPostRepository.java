package com.byeolnight.domain.repository.post;

import com.byeolnight.domain.entity.post.PopularPost;
import com.byeolnight.domain.entity.post.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PopularPostRepository extends JpaRepository<PopularPost, Long> {
    boolean existsByPost(Post post);
    void deleteByPost(Post post);

    @Query("""
        SELECT p.id FROM Post p
        WHERE p.isDeleted = false
        AND SIZE(p.likes) >= 10
        AND p.createdAt >= :threshold
        ORDER BY SIZE(p.likes) DESC, p.createdAt DESC
    """)
    List<Long> findPopularPostIdsSince(@Param("threshold") LocalDateTime threshold);
}
