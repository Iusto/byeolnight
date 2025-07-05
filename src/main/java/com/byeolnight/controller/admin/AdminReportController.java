package com.byeolnight.controller.admin;

import com.byeolnight.dto.admin.ReportedPostDetailDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.admin.AdminReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "👮 관리자 API - 신고", description = "신고 처리 및 관리 API")
public class AdminReportController {

    private final AdminReportService adminReportService;

    @Operation(summary = "신고된 게시글 목록 조회", description = "신고된 게시글 목록을 조회합니다.")
    @GetMapping("/posts")
    public ResponseEntity<CommonResponse<Page<ReportedPostDetailDto>>> getReportedPosts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String searchType,
            Pageable pageable) {
        Page<ReportedPostDetailDto> reportedPosts = adminReportService.getReportedPosts(search, searchType, pageable);
        return ResponseEntity.ok(CommonResponse.success(reportedPosts));
    }

    @Operation(summary = "신고 통계", description = "신고 사유별 통계를 조회합니다.")
    @GetMapping("/stats")
    public ResponseEntity<CommonResponse<Map<String, Long>>> getReportStats() {
        Map<String, Long> stats = adminReportService.getReportStatsByReason();
        return ResponseEntity.ok(CommonResponse.success(stats));
    }

    @Operation(summary = "신고 승인", description = "신고를 승인하고 해당 게시글을 블라인드 처리합니다.")
    @PostMapping("/{reportId}/approve")
    public ResponseEntity<CommonResponse<Void>> approveReport(@PathVariable Long reportId) {
        adminReportService.approveReport(reportId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    @Operation(summary = "신고 거부", description = "신고를 거부하고 허위 신고로 처리합니다.")
    @PostMapping("/{reportId}/reject")
    public ResponseEntity<CommonResponse<Void>> rejectReport(@PathVariable Long reportId) {
        adminReportService.rejectReport(reportId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}