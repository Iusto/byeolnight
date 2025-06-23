package com.byeolnight.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    private String roomId;
    private String sender;
    private String message;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now(); // 기본값 설정
}
