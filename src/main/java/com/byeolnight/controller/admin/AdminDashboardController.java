package com.byeolnight.controller.admin;

import com.byeolnight.dto.admin.AdminDashboardReportStatsDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.admin.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "👮 관리자 API - 대시보드", description = "관리자 대시보드 통계 API")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(summary = "미처리 신고 통계 조회", description = "검토가 완료되지 않은 게시글·댓글 신고 수를 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/report-stats")
    public ResponseEntity<CommonResponse<AdminDashboardReportStatsDto>> getPendingReportStats() {
        return ResponseEntity.ok(CommonResponse.success(adminDashboardService.getPendingReportStats()));
    }
}
