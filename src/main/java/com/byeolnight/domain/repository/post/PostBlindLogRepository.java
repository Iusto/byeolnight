package com.byeolnight.domain.repository.post;

import com.byeolnight.domain.entity.post.PostBlindLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostBlindLogRepository extends JpaRepository<PostBlindLog, Long> {
    boolean existsByPostId(Long postId);
}