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
    private String id; // 메시지 ID 추가
    private String roomId;
    private String sender;
    private String senderIcon;
    private String message;
    private String ipAddress;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    @Builder.Default
    private Boolean isBlinded = false; // 블라인드 상태 추가
}
