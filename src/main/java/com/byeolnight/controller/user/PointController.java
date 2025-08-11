package com.byeolnight.controller.user;

import com.byeolnight.entity.user.User;
import com.byeolnight.dto.user.PointHistoryDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.user.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member/points")
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
@Tag(name = "👤 회원 API - 포인트", description = "스텔라 포인트 관리 및 출석 체크 API")
public class PointController {

    private final PointService pointService;

    @Operation(summary = "출석 체크", description = """
    일일 출석 체크를 하고 스텔라 포인트를 지급받습니다.
    
    🎆 보상 시스템:
    - 기본 출석: 10 포인트
    - 연속 7일 출석: +50 포인트 보너스
    - 하루 1회만 가능
    """)
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "출석 체크 성공 (true) 또는 이미 체크함 (false)"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/attendance")
    public ResponseEntity<CommonResponse<Boolean>> checkAttendance(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        boolean success = pointService.checkDailyAttendance(user);
        CommonResponse<Boolean> response = CommonResponse.success(success);
        log.info("출석 체크 API 응답 - 사용자: {}, 성공: {}", user.getNickname(), success);
        log.info("출석 체크 응답 구조: success={}, data={}", response.isSuccess(), response.getData());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "포인트 히스토리 조회", description = "전체 포인트 히스토리를 페이징으로 조회합니다. (획득 + 사용 내역 모두 포함)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/history")
    public ResponseEntity<CommonResponse<Page<PointHistoryDto>>> getPointHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(hidden = true) Pageable pageable) {
        Page<PointHistoryDto> history = pointService.getPointHistory(user, pageable);
        return ResponseEntity.ok(CommonResponse.success(history));
    }

    @Operation(summary = "포인트 획득 히스토리", description = "포인트 획득 내역만 필터링하여 조회합니다. (출석, 게시글 작성, 관리자 지급 등)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/history/earned")
    public ResponseEntity<CommonResponse<Page<PointHistoryDto>>> getEarnedPointHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(hidden = true) Pageable pageable) {
        Page<PointHistoryDto> history = pointService.getEarnedPointHistory(user, pageable);
        return ResponseEntity.ok(CommonResponse.success(history));
    }

    @Operation(summary = "포인트 사용 히스토리", description = "포인트 사용 내역만 필터링하여 조회합니다. (스텔라 아이콘 구매 등)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/history/spent")
    public ResponseEntity<CommonResponse<Page<PointHistoryDto>>> getSpentPointHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(hidden = true) Pageable pageable) {
        Page<PointHistoryDto> history = pointService.getSpentPointHistory(user, pageable);
        return ResponseEntity.ok(CommonResponse.success(history));
    }

    @Operation(summary = "오늘 출석 여부 확인", description = "오늘 출석 체크 여부를 확인합니다. (UI에서 버튼 상태 표시용)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/attendance/today")
    public ResponseEntity<CommonResponse<Boolean>> getTodayAttendance(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        boolean attended = pointService.isTodayAttended(user);
        return ResponseEntity.ok(CommonResponse.success(attended));
    }
    
    @Operation(summary = "포인트 히스토리 디버그", description = "디버깅용 포인트 히스토리 조회 (페이징 없이 전체 데이터)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/history/debug")
    public ResponseEntity<CommonResponse<java.util.List<com.byeolnight.dto.user.PointHistoryDto>>> getPointHistoryDebug(
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        java.util.List<com.byeolnight.dto.user.PointHistoryDto> history = pointService.getUserPointHistory(user);
        return ResponseEntity.ok(CommonResponse.success(history));
    }
}