package com.byeolnight.domain.repository.post;

import com.byeolnight.domain.entity.post.Post;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import java.util.List;

public interface BlindedPostRepository extends Repository<Post, Long> {

    @Query("""
        SELECT p FROM Post p
        WHERE p.blinded = true
        AND p.isDeleted = false
        ORDER BY p.updatedAt DESC
    """)
    List<Post> findAllBlindedPosts();
}
