package com.byeolnight.dto.admin;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ReportedCommentDetailDto {
    private Long commentId;
    private String content;
    private String writer;
    private String postTitle;
    private Long postId;
    private LocalDateTime createdAt;
    private boolean blinded;
    private int reportCount;
    private List<ReportDetail> reportDetails;

    @Getter
    @Builder
    public static class ReportDetail {
        private Long reportId;
        private String reporterNickname;
        private String reason;
        private String description;
        private LocalDateTime reportedAt;
    }
}