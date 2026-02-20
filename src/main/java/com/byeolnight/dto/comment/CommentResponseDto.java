package com.byeolnight.dto.comment;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CommentResponseDto {
    private Long id;
    private String content;
    private String writer;
    private Long writerId;
    private Boolean blinded;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private Long parentId;
    private String parentWriter;
    private String writerIcon;
    private List<String> writerCertificates;
    private int likeCount;
    private int reportCount;
    private boolean isPopular;
}
