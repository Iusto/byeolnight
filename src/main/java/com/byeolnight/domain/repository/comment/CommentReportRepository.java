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
    
    Optional<CommentReport> findByCommentAndReporter(Comment comment, User reporter);
    
    boolean existsByCommentAndReporter(Comment comment, User reporter);
    
    // user_id 필드로 조회하는 메서드 추가
    @Query("SELECT cr FROM CommentReport cr WHERE cr.comment = :comment AND cr.reporter = :reporter")
    Optional<CommentReport> findByCommentAndUser(Comment comment, User reporter);
    
    @Query("SELECT CASE WHEN COUNT(cr) > 0 THEN true ELSE false END FROM CommentReport cr WHERE cr.comment = :comment AND cr.reporter = :reporter")
    boolean existsByCommentAndUser(Comment comment, User reporter);
    
    List<CommentReport> findByCommentAndStatus(Comment comment, CommentReport.ReportStatus status);
    
    @Query("SELECT cr FROM CommentReport cr WHERE cr.status = 'PENDING' ORDER BY cr.createdAt DESC")
    List<CommentReport> findPendingReports();
}