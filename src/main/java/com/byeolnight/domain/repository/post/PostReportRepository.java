package com.byeolnight.domain.repository.post;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.post.PostReport;
import com.byeolnight.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostReportRepository extends JpaRepository<PostReport, Long> {

    boolean existsByUserAndPost(User user, Post post);

    long countByPost(Post post);
    
    // 관리자용 신고 조회
    List<PostReport> findAllByOrderByCreatedAtDesc();
    
    List<PostReport> findByPostTitleContainingIgnoreCase(String title);
    
    List<PostReport> findByPostWriterNicknameContainingIgnoreCase(String nickname);
}
