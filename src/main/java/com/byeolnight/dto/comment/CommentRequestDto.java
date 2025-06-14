package com.byeolnight.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentRequestDto {

    @NotNull
    private Long postId;

    @NotBlank
    private String content;

    // ✅ 대댓글일 경우 부모 댓글 ID (없으면 null → 최상위 댓글)
    private Long parentId;
}
