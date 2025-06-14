package com.byeolnight.domain.repository;

import com.byeolnight.domain.entity.post.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByCategory(Post.Category category);

    List<Post> findAllByOrderByCreatedAtDesc();

    @Query("SELECT p FROM Post p JOIN FETCH p.writer WHERE p.isDeleted = false")
    Page<Post> findAllWithWriterByIsDeletedFalse(Pageable pageable);
}