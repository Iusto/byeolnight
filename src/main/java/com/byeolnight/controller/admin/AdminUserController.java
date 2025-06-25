package com.byeolnight.controller.admin;

import com.byeolnight.dto.admin.IpBlockRequestDto;
import com.byeolnight.dto.admin.UserStatusChangeRequestDto;
import com.byeolnight.dto.user.UserSummaryDto;
import com.byeolnight.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
public class AdminUserController {

    private final UserService userService;
    private final StringRedisTemplate redisTemplate;

    @Operation(summary = "전체 사용자 요약 조회", description = "관리자 권한으로 전체 사용자 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserSummaryDto.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<List<UserSummaryDto>> getAllUsers() {
        List<UserSummaryDto> users = userService.getAllUserSummaries();
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
    public ResponseEntity<Void> lockUser(@PathVariable Long id) {
        userService.lockUserAccount(id);
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
            @RequestBody UserStatusChangeRequestDto request
    ) {
        userService.changeUserStatus(userId, request.getStatus(), request.getReason());
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
            @RequestParam(required = false) String reason
    ) {
        userService.withdraw(userId, reason != null ? reason : "관리자에 의한 탈퇴 처리");
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "차단된 IP 목록 조회", description = "로그인 실패 등으로 인해 차단된 IP 주소 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/blocked-ips")
    public ResponseEntity<List<String>> getBlockedIps() {
        Set<String> keys = redisTemplate.keys("blocked-ip:*");
        if (keys == null || keys.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<String> ipList = keys.stream()
                .map(key -> key.replace("blocked-ip:", ""))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ipList);
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
    @DeleteMapping("/blocked-ips/{ip}")
    public ResponseEntity<Void> unblockIp(@PathVariable String ip) {
        redisTemplate.delete("blocked-ip:" + ip);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "특정 IP 수동 차단", description = "관리자가 특정 IP를 수동으로 차단합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "차단 성공"),
            @ApiResponse(responseCode = "400", description = "IP 형식 오류")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/blocked-ips")
    public ResponseEntity<Void> blockIpManually(@RequestBody IpBlockRequestDto request) {
        String ip = request.getIp();
        long duration = request.getDurationMinutes();

        if (!ip.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
            return ResponseEntity.badRequest().build();
        }

        redisTemplate.opsForValue().set("blocked-ip:" + ip, "true", duration, TimeUnit.MINUTES);
        return ResponseEntity.ok().build();
    }
}
