package com.byeolnight.domain.entity.chat;

import com.byeolnight.dto.chat.ChatMessageDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomId;  // 기존 @ManyToOne → 단순 필드로 변경
    private String sender;
    private String message;
    private LocalDateTime timestamp;

    public ChatMessageEntity(ChatMessageDto dto) {
        this.roomId = dto.getRoomId();
        this.sender = dto.getSender();
        this.message = dto.getMessage();
        this.timestamp = dto.getTimestamp();
    }
}
