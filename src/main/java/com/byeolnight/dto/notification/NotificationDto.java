package com.byeolnight.dto.notification;

import com.byeolnight.domain.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class NotificationDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Notification.NotificationType type;
        private String title;
        private String message;
        private String targetUrl;
        private Long relatedId;
        private Boolean isRead;
        private LocalDateTime createdAt;

        public static Response from(Notification notification) {
            return Response.builder()
                    .id(notification.getId())
                    .type(notification.getType())
                    .title(notification.getTitle())
                    .message(notification.getMessage())
                    .targetUrl(notification.getTargetUrl())
                    .relatedId(notification.getRelatedId())
                    .isRead(notification.getIsRead())
                    .createdAt(notification.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private List<Response> notifications;
        private long totalCount;
        private int currentPage;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnreadCountResponse {
        private long count;
    }
}