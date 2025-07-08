package com.byeolnight.dto.comment;

import com.byeolnight.domain.entity.comment.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class CommentDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long postId;
        private String postTitle;
        private String content;
        private String writerNickname;
        private Long parentId;
        private boolean isBlinded;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(Comment comment) {
            try {
                return Response.builder()
                        .id(comment.getId())
                        .postId(comment.getPost().getId())
                        .postTitle(comment.getPost().getTitle())
                        .content(comment.getContent())
                        .writerNickname(comment.getWriter().getNickname())
                        .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                        .isBlinded(false) // TODO: Comment 엔티티에 blinded 필드 추가 필요
                        .createdAt(comment.getCreatedAt())
                        .updatedAt(null) // TODO: Comment 엔티티에 updatedAt 필드 추가 필요
                        .build();
            } catch (Exception e) {
                // 삭제된 게시글에 대한 댓글인 경우 기본값 반환
                return Response.builder()
                        .id(comment.getId())
                        .postId(null)
                        .postTitle("삭제된 게시글")
                        .content(comment.getContent())
                        .writerNickname(comment.getWriter().getNickname())
                        .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                        .isBlinded(false)
                        .createdAt(comment.getCreatedAt())
                        .updatedAt(null)
                        .build();
            }
        }
    }
}