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
    private Long postId; // 게시글 ID
    private String title;
    private String content;
    private String writer;
    private String category;
    private int reportCount; // 신고 개수
    private boolean blinded;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    private List<String> reportReasons; // 신고 사유 목록
    private List<ReportDetail> reportDetails; // 상세 신고 정보
    
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
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent().length() > 100 ? post.getContent().substring(0, 100) + "..." : post.getContent())
                .writer(post.getWriter() != null ? post.getWriter().getNickname() : "알 수 없는 사용자")
                .category(post.getCategory().name())
                .reportCount((int)reportCount)
                .blinded(post.isBlinded())
                .createdAt(post.getCreatedAt())
                .reportReasons(reportReasons)
                .reportDetails(reportDetails)
                .build();
    }
}