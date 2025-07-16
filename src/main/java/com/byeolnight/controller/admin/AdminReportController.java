package com.byeolnight.controller.admin;

import com.byeolnight.dto.ApiResponse;
import com.byeolnight.infrastructure.security.JwtTokenProvider;
import com.byeolnight.service.admin.AdminReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reports")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "👮 관리자 API - 신고 관리", description = "신고 처리 관리 API")
public class AdminReportController {

    private final AdminReportService adminReportService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "신고 승인", description = "신고를 승인 처리합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{reportId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveReport(
            @PathVariable Long reportId,
            HttpServletRequest request
    ) {
        Long adminId = jwtTokenProvider.getUserIdFromRequest(request);
        adminReportService.approveReport(reportId, adminId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "신고 거부", description = "신고를 거부 처리합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{reportId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectReport(
            @PathVariable Long reportId,
            @RequestBody RejectReportRequest request,
            HttpServletRequest httpRequest
    ) {
        Long adminId = jwtTokenProvider.getUserIdFromRequest(httpRequest);
        adminReportService.rejectReport(reportId, adminId, request.getReason());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "신고된 댓글 목록 조회", description = "신고된 댓글 목록을 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/comments")
    public ResponseEntity<ApiResponse<?>> getReportedComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var result = adminReportService.getReportedComments(page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    public static class RejectReportRequest {
        private String reason;
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}