// ✅ ChatController.java
package com.byeolnight.controller.chat;

import com.byeolnight.domain.entity.user.User;
import com.byeolnight.dto.chat.ChatMessageDto;
import com.byeolnight.infrastructure.util.IpUtil;
import com.byeolnight.service.chat.ChatService;
import com.byeolnight.service.chat.AdminChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Controller
@Tag(name = "💬 채팅 API", description = "WebSocket 기반 실시간 채팅 API")
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
        
        // IP 차단 확인
        if (adminChatService.isIpBlocked(clientIp)) {
            log.warn("차단된 IP에서 채팅 시도: {}", clientIp);
            return;
        }
        
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof User user) {
            chatMessage.setSender(user.getNickname());  // ✅ 이메일 대신 닉네임 사용
            
            // 채팅 금지 사용자 확인
            if (adminChatService.isUserBanned(user.getNickname())) {
                log.warn("채팅 금지된 사용자의 메시지 차단: {}", user.getNickname());
                return; // 메시지 전송 차단
            }
        } else {
            log.warn("⚠️ principal is null or invalid. fallback to sender from payload: {}", chatMessage.getSender());
        }

        chatService.save(chatMessage, clientIp);
        messagingTemplate.convertAndSend("/topic/public", chatMessage);
    }

    @MessageMapping("/chat.dm.{to}")
    public void sendDirectMessage(@DestinationVariable String to, @Payload ChatMessageDto chatMessage, Principal principal) {
        if (principal != null) {
            chatMessage.setSender(principal.getName());
        } else {
            log.warn("⚠️ principal is null. fallback to sender from payload: {}", chatMessage.getSender());
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

    @Operation(summary = "채팅 메시지 조회", description = "최근 채팅 메시지를 조회합니다.")
    @GetMapping("/api/public/chat")
    public ResponseEntity<List<ChatMessageDto>> getMessages(
            @RequestParam String roomId,
            @RequestParam(defaultValue = "20") int limit) {
        List<ChatMessageDto> messages = chatService.getRecentMessages(roomId, limit);
        return ResponseEntity.ok(messages);
    }
    
    @Operation(summary = "채팅 이력 조회", description = "특정 시점 이전의 채팅 이력을 조회합니다.")
    @GetMapping("/api/public/chat/history")
    public ResponseEntity<List<ChatMessageDto>> getChatHistory(
            @RequestParam String roomId,
            @RequestParam String beforeId,
            @RequestParam(defaultValue = "20") int limit) {
        List<ChatMessageDto> messages = chatService.getMessagesBefore(roomId, beforeId, limit);
        return ResponseEntity.ok(messages);
    }
}