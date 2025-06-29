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
    private Boolean blinded;
    private LocalDateTime createdAt;

    public static CommentResponseDto from(Comment comment) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getBlinded() ? "[블라인드 처리된 댓글입니다]" : comment.getContent())
                .writer(comment.getWriter().getNickname())
                .blinded(comment.getBlinded())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}