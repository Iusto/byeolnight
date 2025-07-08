package com.byeolnight.domain.repository.post;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.post.PostReport;
import com.byeolnight.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostReportRepository extends JpaRepository<PostReport, Long> {

    boolean existsByUserAndPost(User user, Post post);

    long countByPost(Post post);
    
    List<PostReport> findByPost(Post post);
    
    // 관리자용 신고 조회
    List<PostReport> findAllByOrderByCreatedAtDesc();
    
    List<PostReport> findByPostTitleContainingIgnoreCase(String title);
    
    List<PostReport> findByPostWriterNicknameContainingIgnoreCase(String nickname);
    
    @Query("SELECT CONCAT(pr.reason, CASE WHEN pr.description IS NOT NULL AND pr.description != '' THEN CONCAT(' (', pr.description, ')') ELSE '' END) FROM PostReport pr WHERE pr.post = :post")
    List<String> findReasonsByPost(@Param("post") Post post);
    
    @Query("SELECT pr FROM PostReport pr JOIN FETCH pr.user WHERE pr.post = :post ORDER BY pr.createdAt DESC")
    List<PostReport> findDetailsByPost(@Param("post") Post post);
    
    // 사용자별 승인된 신고 수 조회
    @Query("SELECT COUNT(pr) FROM PostReport pr WHERE pr.user = :user AND pr.accepted = true")
    long countApprovedReportsByUser(@Param("user") User user);
    
    // 사용자별 총 신고 수 조회
    long countByUser(User user);
}
