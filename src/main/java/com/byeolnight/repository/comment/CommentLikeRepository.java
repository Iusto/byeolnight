package com.byeolnight.repository.comment;

import com.byeolnight.entity.comment.Comment;
import com.byeolnight.entity.comment.CommentLike;
import com.byeolnight.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    
    boolean existsByCommentAndUser(Comment comment, User user);
    
    void deleteByCommentAndUser(Comment comment, User user);
}