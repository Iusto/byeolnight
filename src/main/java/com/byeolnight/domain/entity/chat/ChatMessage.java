package com.byeolnight.domain.entity.chat;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "chat_messages",
    indexes = {
        @Index(name = "idx_chat_room_timestamp", columnList = "roomId, timestamp"),
        @Index(name = "idx_chat_timestamp", columnList = "timestamp")
    }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String roomId;

    @Column(nullable = false)
    private String sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(length = 45) // IPv4: 15자, IPv6: 45자
    private String ipAddress;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isBlinded = false;

    private Long blindedBy;

    private LocalDateTime blindedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // 메시지 블라인드 처리
    public void blind(Long adminId) {
        this.isBlinded = true;
        this.blindedBy = adminId;
        this.blindedAt = LocalDateTime.now();
    }

    // 메시지 블라인드 해제
    public void unblind() {
        this.isBlinded = false;
        this.blindedBy = null;
        this.blindedAt = null;
    }
}