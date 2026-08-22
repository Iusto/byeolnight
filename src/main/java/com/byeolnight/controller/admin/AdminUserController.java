package com.byeolnight.controller.admin;

import com.byeolnight.dto.admin.UserStatusChangeRequestDto;
import com.byeolnight.dto.user.UserSummaryDto;
import com.byeolnight.entity.user.User;
import com.byeolnight.service.user.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "👮 관리자 API - 사용자", description = "사용자 관리 및 제재 관련 API")
public class AdminUserController {

    private final UserAdminService userAdminService;

    @Operation(summary = "전체 사용자 요약 조회", description = "관리자 권한으로 전체 사용자 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserSummaryDto.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<List<UserSummaryDto>> getAllUsers() {
        List<UserSummaryDto> users = userAdminService.getAllUserSummaries();
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "사용자 계정 잠금", description = "관리자 권한으로 특정 사용자의 계정을 잠금 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "계정 잠금 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{id}/lock")
    public ResponseEntity<Void> lockUser(@PathVariable Long id, 
                                        @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {
        // 자기 자신의 계정은 잠금할 수 없음
        if (currentUser.getId().equals(id)) {
            return ResponseEntity.badRequest().build();
        }
        userAdminService.lockUserAccount(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "사용자 계정 잠금 해제", description = "관리자 권한으로 특정 사용자의 계정 잠금을 해제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "계정 잠금 해제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{id}/unlock")
    public ResponseEntity<Void> unlockUser(@PathVariable Long id, 
                                          @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {
        userAdminService.unlockUserAccount(id);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "회원 상태 변경",
            description = "관리자가 회원의 상태를 변경합니다. 예: ACTIVE, BANNED, SUSPENDED, WITHDRAWN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<Void> changeUserStatus(
            @PathVariable Long userId,
            @RequestBody UserStatusChangeRequestDto request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser
    ) {
        // 자기 자신의 계정 상태는 변경할 수 없음
        if (currentUser.getId().equals(userId)) {
            return ResponseEntity.badRequest().build();
        }
        userAdminService.changeUserStatus(userId, request.getStatus(), request.getReason());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "회원 강제 탈퇴", description = "관리자가 특정 회원을 탈퇴 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 탈퇴 처리 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> forceWithdrawUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String reason,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser
    ) {
        // 자기 자신의 계정은 탈퇴시킬 수 없음
        if (currentUser.getId().equals(userId)) {
            return ResponseEntity.badRequest().build();
        }
        userAdminService.withdraw(userId, reason != null ? reason : "관리자에 의한 탈퇴 처리");
        return ResponseEntity.ok().build();
    }


}
