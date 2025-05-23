
package com.byeolnight.domain.entity.chat;

import jakarta.persistence.*;
import com.byeolnight.domain.entity.user.User;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Lob
    @Column(nullable = false)
    private String content;

    private LocalDateTime sentAt = LocalDateTime.now();

    // Getters and Setters omitted for brevity
}
