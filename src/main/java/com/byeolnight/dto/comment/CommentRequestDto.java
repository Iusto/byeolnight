package com.byeolnight.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentRequestDto {

    private Long postId;  // 수정 시에는 필수가 아님

    @NotBlank
    private String content;
    
    // 답글인 경우 부모 댓글 ID (선택사항)
    private Long parentId;
    
    public CommentRequestDto(Long postId, String content, Long parentId) {
        this.postId = postId;
        this.content = content;
        this.parentId = parentId;
    }
}