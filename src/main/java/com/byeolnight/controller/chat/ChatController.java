// ✅ ChatController.java
package com.byeolnight.controller.chat;

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
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = "💬 채팅 API", description = "WebSocket(STOMP) 기반 실시간 채팅 시스템 API")
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final AdminChatService adminChatService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageDto chatMessage, Principal principal, 
                           org.springframework.messaging.simp.stomp.StompHeaderAccessor headerAccessor) {
        // IP 주소 추출
        String clientIp = "unknown";
        try {
            Object nativeHeaders = headerAccessor.getSessionAttributes().get("clientIp");
            if (nativeHeaders != null) {
                clientIp = nativeHeaders.toString();
            }
        } catch (Exception e) {
            log.debug("IP 추출 실패, 기본값 사용: {}", e.getMessage());
        }
        
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof User user) {
            chatMessage.setSender(user.getNickname());  // ✅ 이메일 대신 닉네임 사용
            
            // ✅ 채팅 금지 사용자 확인 - 메시지 저장 및 전송 전에 먼저 확인
            if (adminChatService.isUserBanned(user.getNickname())) {
                log.warn("밴된 사용자 채팅 시도 차단: {} - 메시지: {}", user.getNickname(), chatMessage.getMessage());
                // 사용자에게 금지 상태 알림
                messagingTemplate.convertAndSendToUser(user.getNickname(), "/queue/ban-notification", 
                    java.util.Map.of("error", "채팅이 제한되어 메시지를 보낼 수 없습니다."));
                return; // 메시지 전송 차단
            }
        } else {
            return; // 인증되지 않은 사용자는 메시지 전송 불가
        }

        chatService.save(chatMessage, clientIp);
        messagingTemplate.convertAndSend("/topic/public", chatMessage);
    }

    @MessageMapping("/chat.dm.{to}")
    public void sendDirectMessage(@DestinationVariable String to, @Payload ChatMessageDto chatMessage, Principal principal) {
        if (principal != null) {
            chatMessage.setSender(principal.getName());
        } else {
            // 인증되지 않은 사용자는 DM 전송 불가
            return;
        }

        chatService.save(chatMessage);
        messagingTemplate.convertAndSend("/queue/user." + to, chatMessage);
    }

    // ✅ 추가된 핸들러: 초기 채팅 내역 전송
    @MessageMapping("/chat.init")
    public void initChat(Principal principal) {
        String sender = (principal != null) ? principal.getName() : "익명";
        List<ChatMessageDto> history = chatService.getRecentMessages("public");
        messagingTemplate.convertAndSendToUser(sender, "/queue/init", history);
    }

    @Operation(summary = "채팅 메시지 조회", description = "특정 채팅방의 최근 메시지를 조회합니다. (비회원 접근 가능)")
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
    
    @Operation(summary = "채팅 이력 조회", description = "특정 메시지 ID 이전의 채팅 이력을 조회합니다. (무한 스크롤용)")
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
    public ResponseEntity<CommonResponse<java.util.Map<String, Object>>> getChatBanStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        java.util.Map<String, Object> banStatus = adminChatService.getUserBanStatus(user.getNickname());
        return ResponseEntity.ok(CommonResponse.success(banStatus));
    }
}