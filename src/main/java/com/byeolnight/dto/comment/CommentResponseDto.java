package com.byeolnight.dto.comment;

import com.byeolnight.domain.entity.comment.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponseDto {
    private Long id;
    private String content;
    private String writer;
    private LocalDateTime createdAt;

    public static CommentResponseDto from(Comment comment) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .writer(comment.getWriter().getNickname())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}