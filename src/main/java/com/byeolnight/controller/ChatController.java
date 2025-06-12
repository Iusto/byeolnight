package com.byeolnight.controller;

import com.byeolnight.dto.chat.ChatMessageDto;
import com.byeolnight.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@RequiredArgsConstructor
@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageDto chatMessage, Principal principal) {
        if (principal != null) {
            chatMessage.setSender(principal.getName());
        } else {
            // 🔥 임시 fallback 처리 (테스트용, 실무에서는 제거해야 함)
            log.warn("⚠️ principal is null. fallback to sender from payload: {}", chatMessage.getSender());
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
}
