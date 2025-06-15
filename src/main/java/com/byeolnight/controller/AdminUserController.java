package com.byeolnight.controller;

import com.byeolnight.dto.admin.UserStatusChangeRequest;
import com.byeolnight.dto.user.UserSummaryDto;
import com.byeolnight.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ✅ 관리자 전용 사용자 관리 API 컨트롤러
 *
 * - ADMIN Role을 가진 사용자만 접근 가능 (@PreAuthorize + Spring Security 설정 필요)
 * - Swagger 문서에 bearerAuth 인증 명시
 * - 운영 중 사용자 모니터링 및 계정 제재 기능을 확장하기 위한 기본 구조
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@SecurityRequirement(name = "bearerAuth") // Swagger 문서에서 인증 헤더 자동 포함
public class AdminUserController {

    private final UserService userService;

    /**
     * ✅ 전체 사용자 요약 정보 조회
     *
     * - ADMIN 권한 사용자만 사용 가능
     * - UserSummaryDto를 통해 개인정보 없이 사용자 개요만 제공
     * - 추후: 정렬/검색/필터 조건 확장 가능
     */
    @Operation(summary = "전체 사용자 요약 조회", description = "관리자 권한으로 전체 사용자 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserSummaryDto.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')") // Security 설정 + Role Enum이 반드시 연동되어 있어야 함
    @GetMapping("/users")
    public ResponseEntity<List<UserSummaryDto>> getAllUsers() {
        List<UserSummaryDto> users = userService.getAllUserSummaries();
        return ResponseEntity.ok(users);
    }

    /**
     * ✅ 특정 사용자 계정 잠금 처리
     *
     * - ADMIN 권한으로 유저 계정을 비활성화 (isLocked = true)
     */
    @Operation(summary = "사용자 계정 잠금", description = "특정 사용자의 계정을 잠금 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "계정 잠금 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{id}/lock")
    public ResponseEntity<Void> lockUser(@PathVariable Long id) {
        userService.lockUserAccount(id);  // 🔧 서비스 로직 필요
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "관리자 인증", description = "해당 계정이 관리자인지 판단합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("관리자 권한 인증 성공");
    }

    @Operation(summary = "회원 상태 변경", description = "관리자가 회원의 상태를 변경합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<Void> changeUserStatus(
            @PathVariable Long userId,
            @RequestBody UserStatusChangeRequest request
    ) {
        userService.changeUserStatus(userId, request.getStatus(), request.getReason());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "회원 강제 탈퇴", description = "관리자가 특정 회원을 탈퇴 처리합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> forceWithdrawUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String reason
    ) {
        userService.withdraw(userId, reason != null ? reason : "관리자에 의한 탈퇴 처리");
        return ResponseEntity.ok().build();
    }
}
