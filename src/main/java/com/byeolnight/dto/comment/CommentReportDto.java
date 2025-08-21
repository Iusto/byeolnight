package com.byeolnight.dto.comment;

import com.byeolnight.entity.comment.Comment;
import com.byeolnight.entity.comment.CommentReport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentReportDto {
    private Long id;
    private String content;
    private String writer;
    private Long postId;
    private String postTitle;
    private int reportCount;
    private boolean blinded;
    private LocalDateTime createdAt;
    private List<String> reportReasons;
    private List<ReportDetailDto> reportDetails;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportDetailDto {
        private Long reportId;
        private String reporterNickname;
        private String reason;
        private String description;
        private boolean reviewed;
        private Boolean accepted;
        private LocalDateTime reportedAt;

        public static ReportDetailDto from(CommentReport report) {
            return ReportDetailDto.builder()
                    .reportId(report.getId())
                    .reporterNickname(report.getUser().getNickname())
                    .reason(report.getReason())
                    .description(report.getDescription())
                    .reviewed(report.isReviewed())
                    .accepted(report.isReviewed() ? report.isAccepted() : null)
                    .reportedAt(report.getCreatedAt())
                    .build();
        }
    }

    public static CommentReportDto from(Comment comment, List<CommentReport> reports) {
        List<ReportDetailDto> reportDetails = reports.stream()
                .map(ReportDetailDto::from)
                .collect(Collectors.toList());

        List<String> reportReasons = reports.stream()
                .map(CommentReport::getReason)
                .distinct()
                .collect(Collectors.toList());

        return CommentReportDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .writer(comment.getWriter().getNickname())
                .postId(comment.getPost().getId())
                .postTitle(comment.getPost().getTitle())
                .reportCount(comment.getReportCount())
                .blinded(comment.isBlinded())
                .createdAt(comment.getCreatedAt())
                .reportReasons(reportReasons)
                .reportDetails(reportDetails)
                .build();
    }

    public static List<CommentReportDto> fromCommentReports(Map<Comment, List<CommentReport>> commentReportsMap) {
        List<CommentReportDto> result = new ArrayList<>();
        
        for (Map.Entry<Comment, List<CommentReport>> entry : commentReportsMap.entrySet()) {
            Comment comment = entry.getKey();
            List<CommentReport> reports = entry.getValue();
            
            result.add(from(comment, reports));
        }
        
        return result;
    }
}