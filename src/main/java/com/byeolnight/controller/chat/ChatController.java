// ✅ ChatController.java
package com.byeolnight.controller.chat;

import com.byeolnight.domain.entity.user.User;
import com.byeolnight.dto.chat.ChatMessageDto;
import com.byeolnight.service.chat.ChatService;
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
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageDto chatMessage, Principal principal) {
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof User user) {
            chatMessage.setSender(user.getNickname());  // ✅ 이메일 대신 닉네임 사용
        } else {
            log.warn("⚠️ principal is null or invalid. fallback to sender from payload: {}", chatMessage.getSender());
        }

        chatService.save(chatMessage);
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

    @GetMapping("/api/public/chat")
    public ResponseEntity<List<ChatMessageDto>> getMessages(@RequestParam String roomId) {
        List<ChatMessageDto> messages = chatService.getRecentMessages(roomId);
        return ResponseEntity.ok(messages);
    }
}