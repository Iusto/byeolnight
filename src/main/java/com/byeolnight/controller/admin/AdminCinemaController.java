package com.byeolnight.controller.admin;

import com.byeolnight.dto.admin.CinemaStatusDto;
import com.byeolnight.dto.cinema.CinemaCollectionResultDto;
import com.byeolnight.entity.user.User;
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
@Tag(name = "관리자 API - 별빛시네마", description = "별빛시네마 관리 API")
public class AdminCinemaController {
    private final CinemaService cinemaService;

    @Operation(summary = "별빛시네마 수동 생성")
    @PostMapping("/generate-post")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<CinemaCollectionResultDto>> generateCinemaPost(
            @AuthenticationPrincipal User admin) {
        if (admin == null) {
            return ResponseEntity.badRequest().body(CommonResponse.error("관리자 정보가 없습니다."));
        }
        CinemaCollectionResultDto result = cinemaService.createCinemaPostManually(admin);
        boolean completed = result.getStatus() == CinemaCollectionResultDto.Status.CREATED
                || result.getStatus() == CinemaCollectionResultDto.Status.ALREADY_CREATED_TODAY;
        CommonResponse<CinemaCollectionResultDto> response = completed
                ? CommonResponse.success(result, result.getMessage())
                : new CommonResponse<>(false, result.getMessage(), result);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "별빛시네마 상태 조회")
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<CinemaStatusDto>> getCinemaStatus() {
        return ResponseEntity.ok(CommonResponse.success(cinemaService.getCinemaStatus()));
    }
}
