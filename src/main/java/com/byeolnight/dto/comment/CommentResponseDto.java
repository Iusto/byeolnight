package com.byeolnight.dto.comment;

import com.byeolnight.domain.entity.comment.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class CommentResponseDto {

    private Long id;
    private String content;
    private String writer;
    private LocalDateTime createdAt;

    // ✅ 자식 댓글 목록 추가 (재귀적 구조)
    private List<CommentResponseDto> children;

    public static CommentResponseDto from(Comment comment) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .writer(comment.getWriter().getNickname())
                .createdAt(comment.getCreatedAt())
                .children(
                        comment.getChildren() == null ? List.of() :
                                comment.getChildren().stream()
                                        .map(CommentResponseDto::from)
                                        .collect(Collectors.toList())
                )
                .build();
    }
}
