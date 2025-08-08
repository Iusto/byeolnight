package com.byeolnight.repository.comment;

import com.byeolnight.entity.comment.Comment;
import com.byeolnight.entity.comment.CommentReport;
import com.byeolnight.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {
    
    boolean existsByCommentAndUser(Comment comment, User user);
    
    List<CommentReport> findByComment(Comment comment);
    
    @Query("SELECT cr FROM CommentReport cr WHERE cr.reviewed = false ORDER BY cr.createdAt DESC")
    List<CommentReport> findPendingReports();
}