package com.byeolnight.controller.admin;

import com.byeolnight.dto.admin.ChatBanRequestDto;
import com.byeolnight.dto.admin.ChatStatsDto;
import com.byeolnight.infrastructure.util.IpUtil;
import com.byeolnight.service.chat.AdminChatService;
import org.springframework.web.bind.annotation.CrossOrigin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/chat")
@SecurityRequirement(name = "BearerAuth")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
@Tag(name = "👮 관리자 API - 채팅", description = "채팅 관리 및 제재 관련 API")
public class AdminChatController {

    private final AdminChatService adminChatService;

    @Operation(summary = "메시지 블라인드 처리", description = "관리자가 부적절한 메시지를 블라인드 처리합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/blind/{messageId}")
    public ResponseEntity<Void> blindMessage(
            @PathVariable String messageId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.byeolnight.domain.entity.user.User admin
    ) {
        adminChatService.blindMessage(messageId, admin.getId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "메시지 블라인드 해제", description = "관리자가 블라인드된 메시지를 해제합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/blind/{messageId}")
    public ResponseEntity<Void> unblindMessage(
            @PathVariable String messageId
    ) {
        adminChatService.unblindMessage(messageId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "사용자 채팅 금지", description = "관리자가 사용자를 일정 시간 채팅 금지합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/ban")
    public ResponseEntity<Void> banUser(
            @RequestBody ChatBanRequestDto request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.byeolnight.domain.entity.user.User admin,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        String adminIp = IpUtil.getClientIp(httpRequest);
        adminChatService.banUser(request.getUsername(), request.getDuration(), admin.getId(), request.getReason());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "IP 차단", description = "관리자가 특정 IP를 채팅에서 차단합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/ip-block")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<String>> blockIp(
            @RequestParam String ip,
            @RequestParam(defaultValue = "60") int durationMinutes,
            @RequestParam(required = false) String reason,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.byeolnight.domain.entity.user.User admin,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        String adminIp = IpUtil.getClientIp(httpRequest);
        adminChatService.blockIp(ip, durationMinutes, admin.getId(), reason != null ? reason : "관리자에 의한 IP 차단");
        return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success(
            String.format("IP %s가 %d분간 차단되었습니다.", ip, durationMinutes)));
    }

    @Operation(summary = "채팅 금지 해제", description = "관리자가 사용자의 채팅 금지를 해제합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/ban/{userId}")
    public ResponseEntity<Void> unbanUser(@PathVariable String userId) {
        adminChatService.unbanUser(userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "채팅 통계 조회", description = "채팅 관련 통계 정보를 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<ChatStatsDto> getChatStats() {
        ChatStatsDto stats = adminChatService.getChatStats();
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "제재된 사용자 목록", description = "현재 채팅 금지된 사용자 목록을 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/banned-users")
    public ResponseEntity<List<Map<String, Object>>> getBannedUsers(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        List<Map<String, Object>> bannedUsers = adminChatService.getBannedUsers(limit, offset);
        return ResponseEntity.ok(bannedUsers);
    }

    @Operation(summary = "블라인드된 메시지 목록", description = "블라인드 처리된 메시지 목록을 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/blinded-messages")
    public ResponseEntity<List<Map<String, Object>>> getBlindedMessages(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        List<Map<String, Object>> blindedMessages = adminChatService.getBlindedMessages(limit, offset);
        return ResponseEntity.ok(blindedMessages);
    }

    @Operation(summary = "사용자 채팅 금지 상태 확인", description = "현재 로그인한 사용자의 채팅 금지 상태를 확인합니다.")
    @GetMapping("/ban-status")
    public ResponseEntity<Map<String, Object>> getBanStatus(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.byeolnight.domain.entity.user.User user
    ) {
        Map<String, Object> banStatus = adminChatService.getUserBanStatus(user.getNickname());
        return ResponseEntity.ok(banStatus);
    }
}