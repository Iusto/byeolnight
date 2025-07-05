package com.byeolnight.dto.post;

import com.byeolnight.domain.entity.post.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class PostDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String title;
        private String content;
        private String category;
        private String writerNickname;
        private long likeCount;
        private long commentCount;
        private int viewCount;
        private boolean isBlinded;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(Post post, long likeCount, long commentCount) {
            return Response.builder()
                    .id(post.getId())
                    .title(post.getTitle())
                    .content(post.getContent())
                    .category(post.getCategory().name())
                    .writerNickname(post.getWriter().getNickname())
                    .likeCount(likeCount)
                    .commentCount(commentCount)
                    .viewCount(post.getViewCount())
                    .isBlinded(post.isBlinded())
                    .createdAt(post.getCreatedAt())
                    .updatedAt(post.getUpdatedAt())
                    .build();
        }
    }
}