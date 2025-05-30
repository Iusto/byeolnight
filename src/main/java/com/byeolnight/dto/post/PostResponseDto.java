package com.byeolnight.dto.post;

import com.byeolnight.domain.entity.post.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PostResponseDto {

    private final Long id;
    private final String title;
    private final String content;
    private final String writer;
    private final String category;
    private final int viewCount;
    private final LocalDateTime createdAt;

    @Builder
    public PostResponseDto(Long id, String title, String content, String writer, String category, int viewCount, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.writer = writer;
        this.category = category;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
    }

    public static PostResponseDto from(Post post) {
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .writer(post.getWriter().getNickname())
                .category(post.getCategory().name())
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
