package com.byeolnight.domain.repository.comment;

import com.byeolnight.domain.entity.comment.Comment;
import com.byeolnight.domain.entity.comment.CommentLike;
import com.byeolnight.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    
    Optional<CommentLike> findByCommentAndUser(Comment comment, User user);
    
    boolean existsByCommentAndUser(Comment comment, User user);
    
    void deleteByCommentAndUser(Comment comment, User user);
}