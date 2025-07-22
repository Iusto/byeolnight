package com.byeolnight.domain.repository.comment;

import com.byeolnight.domain.entity.comment.Comment;
import com.byeolnight.domain.entity.comment.CommentReport;
import com.byeolnight.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {
    
    Optional<CommentReport> findByCommentAndUser(Comment comment, User user);
    
    boolean existsByCommentAndUser(Comment comment, User user);
    
    List<CommentReport> findByComment(Comment comment);
    
    @Query("SELECT cr FROM CommentReport cr WHERE cr.reviewed = false ORDER BY cr.createdAt DESC")
    List<CommentReport> findPendingReports();
}