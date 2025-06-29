package com.byeolnight.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class MessageDto {
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private Long receiverId;
        private String title;
        private String content;
    }
    
    @Data
    @Builder
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
        private LocalDateTime createdAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Long id;
        private String senderNickname;
        private String title;
        private Boolean isRead;
        private LocalDateTime createdAt;
    }
}