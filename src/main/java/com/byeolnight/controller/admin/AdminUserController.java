package com.byeolnight.controller.admin;

import com.byeolnight.dto.admin.IpBlockRequestDto;
import com.byeolnight.dto.admin.NicknameDebugDto;
import com.byeolnight.dto.admin.UserStatusChangeRequestDto;
import com.byeolnight.dto.admin.PointAwardRequestDto;
import com.byeolnight.dto.user.UserSummaryDto;
import com.byeolnight.entity.user.User;
import com.byeolnight.service.auth.AccountRecoveryService;
import com.byeolnight.service.user.PointService;
import com.byeolnight.service.user.UserAccountService;
import com.byeolnight.service.user.UserAdminService;
import com.byeolnight.service.user.WithdrawnUserCleanupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "👮 관리자 API - 사용자", description = "사용자 관리 및 제재 관련 API")
public class AdminUserController {

    private final UserAdminService userAdminService;
    private final UserAccountService userAccountService;
    private final StringRedisTemplate redisTemplate;
    private final PointService pointService;
    private final WithdrawnUserCleanupService withdrawnUserCleanupService;
    private final AccountRecoveryService accountRecoveryService;

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

    @Operation(summary = "차단된 IP 목록 조회", description = "로그인 실패 등으로 인해 차단된 IP 주소 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/blocked-ips")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<List<String>>> getBlockedIps() {
        Set<String> keys = redisTemplate.keys("blocked:ip:*");
        if (keys == null || keys.isEmpty()) {
            return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success(Collections.emptyList()));
        }
        List<String> ipList = keys.stream()
                .map(key -> key.replace("blocked:ip:", ""))
                .collect(Collectors.toList());
        return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success(ipList));
    }

    @Operation(
            summary = "차단된 IP 해제",
            description = "관리자가 로그인 실패 누적으로 차단된 특정 IP 주소의 차단을 해제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "차단 해제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/blocked-ips")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<String>> unblockIp(@RequestParam String ip) {
        redisTemplate.delete("blocked:ip:" + ip);
        return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success("IP 차단이 해제되었습니다."));
    }

    @Operation(summary = "특정 IP 수동 차단", description = "관리자가 특정 IP를 수동으로 차단합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "차단 성공"),
            @ApiResponse(responseCode = "400", description = "IP 형식 오류")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/blocked-ips")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<String>> blockIpManually(@RequestBody IpBlockRequestDto request) {
        String ip = request.getIp();
        long duration = request.getDurationMinutes();

        if (!ip.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
            return ResponseEntity.badRequest()
                    .body(com.byeolnight.infrastructure.common.CommonResponse.fail("잘못된 IP 형식입니다."));
        }

        redisTemplate.opsForValue().set("blocked:ip:" + ip, "true", duration, TimeUnit.MINUTES);
        return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success("IP가 차단되었습니다."));
    }

    @Operation(summary = "사용자 포인트 수여", description = "관리자가 특정 사용자에게 포인트를 수여합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "포인트 수여 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/{userId}/points")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<String>> awardPoints(
            @PathVariable Long userId,
            @RequestBody @jakarta.validation.Valid PointAwardRequestDto request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser
    ) {
        try {
            pointService.awardPointsByAdmin(userId, request.getPoints(), request.getReason(), currentUser.getId());
            return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success(
                    String.format("%d 포인트가 성공적으로 수여되었습니다.", request.getPoints())));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(com.byeolnight.infrastructure.common.CommonResponse.fail(e.getMessage()));
        }
    }

    @Operation(summary = "기본 소행성 아이콘 마이그레이션", description = "모든 기존 사용자에게 기본 소행성 아이콘을 부여하고 장착합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "마이그레이션 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/migrate-default-icon")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<String>> migrateDefaultAsteroidIcon() {
        try {
            userAccountService.migrateDefaultAsteroidIcon();
            return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success(
                    "모든 사용자에게 기본 소행성 아이콘이 성공적으로 부여되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(com.byeolnight.infrastructure.common.CommonResponse.fail(
                            "마이그레이션 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @Operation(summary = "닉네임 디버깅", description = "특정 닉네임의 존재 여부를 확인합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/debug/nickname/{nickname}")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<NicknameDebugDto>> debugNickname(
            @PathVariable String nickname) {

        // 데이터베이스에서 비슷한 닉네임들 찾기
        java.util.List<String> similarNicknames = userAdminService.getAllUserSummaries().stream()
                .map(UserSummaryDto::getNickname)
                .filter(n -> n.toLowerCase().contains(nickname.toLowerCase()) ||
                           nickname.toLowerCase().contains(n.toLowerCase()))
                .collect(java.util.stream.Collectors.toList());

        NicknameDebugDto result = NicknameDebugDto.builder()
                .inputNickname(nickname)
                .trimmedNickname(nickname.trim())
                .exists(userAccountService.isNicknameDuplicated(nickname))
                .similarNicknames(similarNicknames)
                .build();

        return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success(result));
    }

    @Operation(summary = "탈퇴 회원 정리 (수동 실행)", description = "탈퇴 후 2년 경과한 회원의 개인정보를 수동으로 정리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "정리 완료"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/cleanup-withdrawn")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<String>> cleanupWithdrawnUsers() {
        try {
            withdrawnUserCleanupService.cleanupWithdrawnUsers();
            return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success(
                "탈퇴 회원 정리가 완료되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(com.byeolnight.infrastructure.common.CommonResponse.fail(
                    "탈퇴 회원 정리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @Operation(summary = "닉네임 변경권 수여", description = "관리자가 특정 사용자에게 닉네임 변경권을 수여합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "닉네임 변경권 수여 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/{userId}/nickname-change-ticket")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<String>> grantNicknameChangeTicket(
            @PathVariable Long userId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser
    ) {
        try {
            userAdminService.grantNicknameChangeTicket(userId, currentUser.getId());
            return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success(
                    "닉네임 변경권이 성공적으로 수여되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(com.byeolnight.infrastructure.common.CommonResponse.fail(e.getMessage()));
        }
    }

    @Operation(summary = "탈퇴 계정 복구", description = "관리자가 30일 내 탈퇴한 계정을 복구합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "계정 복구 성공"),
            @ApiResponse(responseCode = "400", description = "복구 불가능한 계정"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/recover")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<String>> recoverWithdrawnAccount(
            @RequestParam String email,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser
    ) {
        try {
            boolean recovered = accountRecoveryService.recoverWithdrawnAccount(email);
            if (recovered) {
                return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success(
                        "계정이 성공적으로 복구되었습니다."));
            } else {
                return ResponseEntity.badRequest()
                        .body(com.byeolnight.infrastructure.common.CommonResponse.fail(
                                "복구할 수 없는 계정입니다. (30일 경과 또는 존재하지 않음)"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(com.byeolnight.infrastructure.common.CommonResponse.fail(
                            "계정 복구 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }


}
