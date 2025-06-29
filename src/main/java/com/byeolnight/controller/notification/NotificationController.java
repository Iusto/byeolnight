package com.byeolnight.controller.notification;

import com.byeolnight.dto.notification.NotificationDto;
import com.byeolnight.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @GetMapping
    public ResponseEntity<Page<NotificationDto.Response>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        Long userId = Long.parseLong(userDetails.getUsername());
        Page<NotificationDto.Response> notifications = notificationService.getNotifications(userId, pageable);
        return ResponseEntity.ok(notifications);
    }
    
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        notificationService.deleteNotification(notificationId, userId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearAllNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        notificationService.clearAllNotifications(userId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }
}