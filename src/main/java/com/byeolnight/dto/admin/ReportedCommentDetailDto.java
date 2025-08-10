package com.byeolnight.dto.admin;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ReportedCommentDetailDto {
    private Long commentId; // 댓글 ID
    private String content;
    private String writer;
    private String postTitle;
    private Long postId;
    private LocalDateTime createdAt;
    private boolean blinded;
    private int reportCount; // 신고 개수
    private List<String> reportReasons; // 신고 사유 목록
    private List<ReportDetail> reportDetails;
    private boolean allProcessed; // 모든 신고가 처리되었는지 여부

    @Getter
    @Builder
    public static class ReportDetail {
        private Long reportId;
        private String reporterNickname;
        private String reason;
        private String description;
        private LocalDateTime reportedAt;
        private boolean reviewed; // 검토 여부
        private boolean accepted; // 승인 여부
    }
}