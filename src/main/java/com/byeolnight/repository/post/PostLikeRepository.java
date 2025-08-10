package com.byeolnight.repository.post;

import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.post.PostLike;
import com.byeolnight.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    boolean existsByUserAndPost(User user, Post post);
    long countByPost(Post post);
}
