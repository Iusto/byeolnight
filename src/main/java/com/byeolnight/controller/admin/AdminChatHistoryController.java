package com.byeolnight.controller.admin;

import com.byeolnight.dto.chat.ChatMessageDto;
import com.byeolnight.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/chat")
public class AdminChatHistoryController {

    private final ChatService chatService;

    /**
     * 관리자용 채팅 내역 조회 (더 많은 메시지)
     */
    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ChatMessageDto>> getChatHistory(
            @RequestParam(defaultValue = "public") String roomId,
            @RequestParam(defaultValue = "100") int limit) {
        
        List<ChatMessageDto> messages = chatService.getRecentMessages(roomId, limit);
        return ResponseEntity.ok(messages);
    }

    /**
     * 블라인드된 메시지만 조회
     */
    @GetMapping("/blinded")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ChatMessageDto>> getBlindedMessages(
            @RequestParam(defaultValue = "50") int limit) {
        
        List<ChatMessageDto> messages = chatService.getBlindedMessages(limit);
        return ResponseEntity.ok(messages);
    }
}