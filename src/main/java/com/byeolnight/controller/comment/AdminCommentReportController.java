package com.byeolnight.controller.comment;

import com.byeolnight.entity.comment.CommentReport;
import com.byeolnight.entity.user.User;
import com.byeolnight.dto.comment.CommentReportDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.comment.CommentReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "👮 관리자 API - 댓글 신고", description = "댓글 신고 관리 API")
public class AdminCommentReportController {

    private final CommentReportService commentReportService;

    @Operation(summary = "신고된 댓글 목록 조회", description = "관리자가 신고된 댓글 목록을 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/comments/reported")
    public ResponseEntity<List<CommentReportDto>> getReportedComments() {
        List<CommentReportDto> reportedComments = commentReportService.getReportedComments();
        return ResponseEntity.ok(reportedComments);
    }

    @Operation(summary = "대기 중인 댓글 신고 목록 조회", description = "관리자가 처리되지 않은 댓글 신고 목록을 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/comment-reports/pending")
    public ResponseEntity<CommonResponse<List<CommentReport>>> getPendingReports() {
        List<CommentReport> reports = commentReportService.getPendingCommentReports();
        return ResponseEntity.ok(CommonResponse.success(reports));
    }

    @Operation(summary = "댓글 신고 승인", description = "관리자가 댓글 신고를 승인하고 해당 댓글을 블라인드 처리합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/comment-reports/{reportId}/approve")
    public ResponseEntity<CommonResponse<Void>> approveReport(
            @PathVariable Long reportId,
            @Parameter(hidden = true) @AuthenticationPrincipal User admin) {
        commentReportService.processCommentReport(reportId, admin, true, null);
        return ResponseEntity.ok(CommonResponse.success());
    }

    @Operation(summary = "댓글 신고 거부", description = "관리자가 댓글 신고를 거부합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/comment-reports/{reportId}/reject")
    public ResponseEntity<CommonResponse<Void>> rejectReport(
            @PathVariable Long reportId,
            @RequestBody RejectReasonRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal User admin) {
        commentReportService.processCommentReport(reportId, admin, false, request.getReason());
        return ResponseEntity.ok(CommonResponse.success());
    }

    public static class RejectReasonRequest {
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}