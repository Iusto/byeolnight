package com.byeolnight.domain.entity.chat;

import com.byeolnight.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_participations", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "participation_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "participation_date", nullable = false)
    private LocalDate participationDate;

    @Column(name = "message_count", nullable = false)
    @Builder.Default
    private Integer messageCount = 1;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static ChatParticipation of(User user, LocalDate date) {
        return ChatParticipation.builder()
                .user(user)
                .participationDate(date)
                .messageCount(1)
                .build();
    }

    public void incrementMessageCount() {
        this.messageCount++;
    }
}