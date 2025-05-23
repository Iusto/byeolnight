package com.byeolnight.domain.repository;

import com.byeolnight.domain.entity.post.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByCategory(Post.Category category);
    List<Post> findAllByOrderByCreatedAtDesc();
}