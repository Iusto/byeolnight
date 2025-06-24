package com.byeolnight.domain.repository.post;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.post.PostLike;
import com.byeolnight.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    boolean existsByUserAndPost(User user, Post post);
    Optional<PostLike> findByUserAndPost(User user, Post post);
    long countByPost(Post post);
}
