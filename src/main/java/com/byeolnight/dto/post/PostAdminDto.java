package com.byeolnight.dto.post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostAdminDto {
    private Long id;
    private String title;
    private String content;
    private String writer;
    private String category;
    private boolean blinded;
    private boolean deleted;
    private int viewCount;
    private int likeCount;
    private int commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
}