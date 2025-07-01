package com.byeolnight.dto.admin;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ReportedPostDetailDto {
    private Long postId;
    private String title;
    private String content;
    private String writer;
    private String category;
    private LocalDateTime createdAt;
    private boolean blinded;
    private int totalReportCount;
    private List<ReportDetail> reports;

    @Getter
    @Builder
    public static class ReportDetail {
        private Long reportId;
        private String reporterNickname;
        private String reason;
        private LocalDateTime reportedAt;
    }
}