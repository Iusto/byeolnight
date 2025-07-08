package com.byeolnight.dto.admin;

import com.byeolnight.domain.entity.post.Post;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ReportedPostDetailDto {
    
    private Long id;
    private String title;
    private String writer;
    private String category;
    private long reportCount;
    private boolean blinded;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    private List<String> reportReasons;
    private List<ReportDetail> reportDetails; // 상세 신고 정보
    
    // AdminReportService용 추가 필드
    private Long postId;
    private String content;
    private int totalReportCount;
    private List<ReportDetail> reports;
    
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ReportDetail {
        private Long reportId;
        private String reporterNickname;
        private String reason;
        private String description;
        private boolean reviewed;
        private boolean accepted;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime reportedAt;
    }
    
    public static ReportedPostDetailDto of(Post post, long reportCount, List<String> reportReasons, List<com.byeolnight.domain.entity.post.PostReport> reports) {
        List<ReportDetail> reportDetails = reports.stream()
                .map(report -> ReportDetail.builder()
                        .reportId(report.getId())
                        .reporterNickname(report.getUser().getNickname())
                        .reason(report.getReason())
                        .description(report.getDescription())
                        .reviewed(report.isReviewed())
                        .accepted(report.isAccepted())
                        .reportedAt(report.getCreatedAt())
                        .build())
                .collect(java.util.stream.Collectors.toList());
        
        return ReportedPostDetailDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .writer(post.getWriter() != null ? post.getWriter().getNickname() : "알 수 없는 사용자")
                .category(post.getCategory().name())
                .reportCount(reportCount)
                .blinded(post.isBlinded())
                .createdAt(post.getCreatedAt())
                .reportReasons(reportReasons)
                .reportDetails(reportDetails)
                .build();
    }
}