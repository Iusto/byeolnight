package com.byeolnight.domain.repository;

import com.byeolnight.domain.entity.post.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByCategory(Post.Category category);

    List<Post> findAllByOrderByCreatedAtDesc();

    // Soft delete 고려한 페이징 조회 메서드
    Page<Post> findAllByIsDeletedFalse(Pageable pageable);
}