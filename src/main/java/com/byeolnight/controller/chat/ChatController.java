package com.byeolnight.controller.chat;

import com.byeolnight.dto.admin.ChatBanStatusDto;
import com.byeolnight.entity.user.User;
import com.byeolnight.dto.chat.ChatMessageDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.chat.ChatService;
import com.byeolnight.service.chat.AdminChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = "💬 채팅 API", description = "Native WebSocket 기반 실시간 채팅 시스템 API")
public class ChatController {

    private final ChatService chatService;
    private final AdminChatService adminChatService;

    @Operation(summary = "채팅 메시지 조회", description = "특정 채팅방의 최근 메시지를 조회합니다. (로그인 불필요)")
    @Parameters({
            @Parameter(name = "roomId", description = "채팅방 ID", example = "public", required = true),
            @Parameter(name = "limit", description = "조회할 메시지 수 (최대 100)", example = "20")
    })
    @GetMapping("/api/public/chat")
    public ResponseEntity<CommonResponse<List<ChatMessageDto>>> getMessages(
            @RequestParam String roomId,
            @RequestParam(defaultValue = "20") int limit) {
        List<ChatMessageDto> messages = chatService.getRecentMessages(roomId, limit);
        return ResponseEntity.ok(CommonResponse.success(messages));
    }
    
    @Operation(summary = "채팅 이력 조회", description = "특정 메시지 ID 이전의 채팅 이력을 조회합니다. (로그인 불필요)")
    @Parameters({
            @Parameter(name = "roomId", description = "채팅방 ID", example = "public", required = true),
            @Parameter(name = "beforeId", description = "기준 메시지 ID (이 ID 이전 메시지들 조회)", example = "msg_123", required = true),
            @Parameter(name = "limit", description = "조회할 메시지 수 (최대 100)", example = "20")
    })
    @GetMapping("/api/public/chat/history")
    public ResponseEntity<CommonResponse<List<ChatMessageDto>>> getChatHistory(
            @RequestParam String roomId,
            @RequestParam String beforeId,
            @RequestParam(defaultValue = "20") int limit) {
        List<ChatMessageDto> messages = chatService.getMessagesBefore(roomId, beforeId, limit);
        return ResponseEntity.ok(CommonResponse.success(messages));
    }

    @Operation(summary = "채팅 금지 상태 조회", description = "현재 사용자의 채팅 금지 상태를 조회합니다. (banned: boolean, reason: string, expiresAt: timestamp)")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/api/member/chat/ban-status")
    public ResponseEntity<CommonResponse<ChatBanStatusDto>> getChatBanStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        com.byeolnight.dto.admin.ChatBanStatusDto banStatus = adminChatService.getUserBanStatus(user.getNickname());
        return ResponseEntity.ok(CommonResponse.success(banStatus));
    }
}