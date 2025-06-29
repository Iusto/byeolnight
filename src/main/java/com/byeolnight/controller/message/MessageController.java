package com.byeolnight.controller.message;

import com.byeolnight.dto.message.MessageDto;
import com.byeolnight.service.message.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {
    
    private final MessageService messageService;
    
    @PostMapping
    public ResponseEntity<MessageDto.Response> sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody MessageDto.Request request) {
        Long senderId = Long.parseLong(userDetails.getUsername());
        MessageDto.Response response = messageService.sendMessage(senderId, request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/received")
    public ResponseEntity<Page<MessageDto.Summary>> getReceivedMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        Long userId = Long.parseLong(userDetails.getUsername());
        Page<MessageDto.Summary> messages = messageService.getReceivedMessages(userId, pageable);
        return ResponseEntity.ok(messages);
    }
    
    @GetMapping("/sent")
    public ResponseEntity<Page<MessageDto.Summary>> getSentMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        Long userId = Long.parseLong(userDetails.getUsername());
        Page<MessageDto.Summary> messages = messageService.getSentMessages(userId, pageable);
        return ResponseEntity.ok(messages);
    }
    
    @GetMapping("/{messageId}")
    public ResponseEntity<MessageDto.Response> getMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        MessageDto.Response message = messageService.getMessage(messageId, userId);
        return ResponseEntity.ok(message);
    }
    
    @PutMapping("/{messageId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        messageService.markAsRead(messageId, userId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        messageService.deleteMessage(messageId, userId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        long count = messageService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }
}