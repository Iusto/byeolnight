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
@Table(name = "chat_bans")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ChatBan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private Long bannedBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime bannedAt;

    @Column(nullable = false)
    private LocalDateTime bannedUntil;

    private String reason;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    // 금지 해제
    public void unban() {
        this.isActive = false;
    }

    // 금지 기간이 만료되었는지 확인
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(bannedUntil);
    }
}