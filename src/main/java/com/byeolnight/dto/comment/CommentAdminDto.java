package com.byeolnight.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentAdminDto {
    private Long id;
    private String content;
    private String originalContent; // 관리자용 원본 내용
    private String writer;
    private String postTitle;
    private Long postId;
    private boolean blinded;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private LocalDateTime blindedAt;
}