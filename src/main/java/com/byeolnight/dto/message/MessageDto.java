package com.byeolnight.dto.message;

import com.byeolnight.entity.Message;
import com.byeolnight.entity.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class MessageDto {

    @Getter
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long senderId;
        private String senderNickname;
        private Long receiverId;
        private String receiverNickname;
        private String title;
        private String content;
        private Boolean isRead;
        private LocalDateTime readAt;
        private LocalDateTime createdAt;

        public static Response from(Message message) {
            String senderNickname = message.getSender().getStatus() == User.UserStatus.WITHDRAWN 
                ? "탈퇴한 사용자" : message.getSender().getNickname();
            String receiverNickname = message.getReceiver().getStatus() == User.UserStatus.WITHDRAWN 
                ? "탈퇴한 사용자" : message.getReceiver().getNickname();
                
            return Response.builder()
                    .id(message.getId())
                    .senderId(message.getSender().getId())
                    .senderNickname(senderNickname)
                    .receiverId(message.getReceiver().getId())
                    .receiverNickname(receiverNickname)
                    .title(message.getTitle())
                    .content(message.getContent())
                    .isRead(message.getIsRead())
                    .readAt(message.getReadAt())
                    .createdAt(message.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendRequest {
        private Long receiverId;
        private String title;
        private String content;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private List<Response> messages;
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