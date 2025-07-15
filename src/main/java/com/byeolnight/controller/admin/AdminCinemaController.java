package com.byeolnight.controller.admin;

import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.cinema.CinemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/cinema")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "👮 관리자 API - 별빛 시네마", description = "별빛 시네마 관리 API")
public class AdminCinemaController {

    private final CinemaService cinemaService;

    @Operation(summary = "별빛 시네마 수동 생성", description = "관리자가 수동으로 별빛 시네마 포스트를 생성합니다.")
    @PostMapping("/generate-post")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<String>> generateCinemaPost(
            @AuthenticationPrincipal com.byeolnight.domain.entity.user.User admin
    ) {
        try {
            if (admin == null) {
                throw new IllegalArgumentException("관리자 정보가 없습니다.");
            }
            cinemaService.createCinemaPostManually(admin);
            return ResponseEntity.ok(CommonResponse.success("별빛 시네마 포스트가 성공적으로 생성되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(CommonResponse.error("별빛 시네마 포스트 생성 실패: " + e.getMessage()));
        }
    }

    @Operation(summary = "별빛 시네마 상태 조회", description = "별빛 시네마 시스템의 상태를 조회합니다.")
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<Object>> getCinemaStatus() {
        try {
            Object status = cinemaService.getCinemaStatus();
            return ResponseEntity.ok(CommonResponse.success(status));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(CommonResponse.error("상태 조회 실패: " + e.getMessage()));
        }
    }
}