package com.byeolnight.controller.admin;

import com.byeolnight.dto.admin.ReportedPostDetailDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.admin.AdminReportService;
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
public class AdminReportController {

    private final AdminReportService adminReportService;

    /**
     * 신고된 게시글 목록 조회
     */
    @GetMapping("/posts")
    public ResponseEntity<CommonResponse<Page<ReportedPostDetailDto>>> getReportedPosts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String searchType,
            Pageable pageable) {
        Page<ReportedPostDetailDto> reportedPosts = adminReportService.getReportedPosts(search, searchType, pageable);
        return ResponseEntity.ok(CommonResponse.success(reportedPosts));
    }

    /**
     * 신고 사유별 통계
     */
    @GetMapping("/stats")
    public ResponseEntity<CommonResponse<Map<String, Long>>> getReportStats() {
        Map<String, Long> stats = adminReportService.getReportStatsByReason();
        return ResponseEntity.ok(CommonResponse.success(stats));
    }

    /**
     * 신고 승인 (유효한 신고로 인정)
     */
    @PostMapping("/{reportId}/approve")
    public ResponseEntity<CommonResponse<Void>> approveReport(@PathVariable Long reportId) {
        adminReportService.approveReport(reportId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    /**
     * 신고 거부 (허위 신고로 판정)
     */
    @PostMapping("/{reportId}/reject")
    public ResponseEntity<CommonResponse<Void>> rejectReport(@PathVariable Long reportId) {
        adminReportService.rejectReport(reportId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}