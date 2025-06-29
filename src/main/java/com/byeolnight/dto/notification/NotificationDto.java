package com.byeolnight.dto.notification;

import com.byeolnight.domain.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class NotificationDto {
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Notification.NotificationType type;
        private String title;
        private String content;
        private Long referenceId;
        private Boolean isRead;
        private LocalDateTime createdAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Settings {
        private Boolean messageNotification;
        private Boolean commentNotification;
        private Boolean replyNotification;
    }
}