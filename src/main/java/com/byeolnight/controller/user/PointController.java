package com.byeolnight.controller.user;

import com.byeolnight.domain.entity.user.User;
import com.byeolnight.dto.user.PointHistoryDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.user.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    /**
     * 출석 체크
     */
    @PostMapping("/attendance")
    public ResponseEntity<CommonResponse<Boolean>> checkAttendance(@AuthenticationPrincipal User user) {
        boolean success = pointService.checkDailyAttendance(user);
        return ResponseEntity.ok(CommonResponse.success(success));
    }

    /**
     * 포인트 히스토리 조회 (전체)
     */
    @GetMapping("/history")
    public ResponseEntity<CommonResponse<Page<PointHistoryDto>>> getPointHistory(
            @AuthenticationPrincipal User user,
            Pageable pageable) {
        Page<PointHistoryDto> history = pointService.getPointHistory(user, pageable);
        return ResponseEntity.ok(CommonResponse.success(history));
    }

    /**
     * 포인트 획득 히스토리 조회
     */
    @GetMapping("/history/earned")
    public ResponseEntity<CommonResponse<Page<PointHistoryDto>>> getEarnedPointHistory(
            @AuthenticationPrincipal User user,
            Pageable pageable) {
        Page<PointHistoryDto> history = pointService.getEarnedPointHistory(user, pageable);
        return ResponseEntity.ok(CommonResponse.success(history));
    }

    /**
     * 포인트 사용 히스토리 조회
     */
    @GetMapping("/history/spent")
    public ResponseEntity<CommonResponse<Page<PointHistoryDto>>> getSpentPointHistory(
            @AuthenticationPrincipal User user,
            Pageable pageable) {
        Page<PointHistoryDto> history = pointService.getSpentPointHistory(user, pageable);
        return ResponseEntity.ok(CommonResponse.success(history));
    }

    /**
     * 오늘 출석 여부 확인
     */
    @GetMapping("/attendance/today")
    public ResponseEntity<CommonResponse<Boolean>> getTodayAttendance(@AuthenticationPrincipal User user) {
        boolean attended = pointService.isTodayAttended(user);
        return ResponseEntity.ok(CommonResponse.success(attended));
    }
}