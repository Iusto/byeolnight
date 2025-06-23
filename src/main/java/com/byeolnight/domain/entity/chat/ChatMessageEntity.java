package com.byeolnight.domain.entity.chat;

import com.byeolnight.dto.chat.ChatMessageDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "chat_messages")
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String roomId;

    @Column(nullable = false)
    private String sender;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public ChatMessageEntity(ChatMessageDto dto) {
        this.roomId = dto.getRoomId();
        this.sender = dto.getSender();
        this.message = dto.getMessage();
        this.timestamp = dto.getTimestamp() != null ? dto.getTimestamp() : LocalDateTime.now();
    }
}
