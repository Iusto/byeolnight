package com.byeolnight.domain.repository.post;

public interface PostRepositoryCustom {
    
    /**
     * 댓글이 없는 게시글 수 조회
     */
    long countPostsWithoutCommentsByUser(Long userId);
    
    /**
     * 이미지가 포함된 게시글 수 조회
     */
    long countImagePostsByUser(Long userId);
    
    /**
     * 특정 좋아요 수 이상의 게시글 수 조회
     */
    long countPostsWithLikesAbove(Long userId, int likeCount);
}