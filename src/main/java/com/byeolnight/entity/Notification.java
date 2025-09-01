package com.byeolnight.entity;

import com.byeolnight.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 알림을 받을 사용자

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false)
    private String title; // 알림 제목

    @Column(nullable = false)
    private String message; // 알림 내용

    @Column
    private String targetUrl; // 클릭 시 이동할 URL

    @Column
    private Long relatedId; // 관련 엔티티 ID (게시글, 댓글 등)

    @Builder.Default
    @Column(nullable = false)
    private Boolean isRead = false; // 읽음 여부

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 읽음 처리
    public void markAsRead() {
        this.isRead = true;
    }

    public enum NotificationType {
        COMMENT_ON_POST,    // 내 게시글에 댓글
        REPLY_ON_COMMENT,   // 내 댓글에 답글
        NEW_MESSAGE,        // 새 쪽지
        NEW_NOTICE,         // 새 공지사항
        SUGGESTION_RESPONSE, // 건의사항 답변
        CELESTIAL_EVENT,    // 천체 이벤트 알림
        WEATHER_ALERT       // 관측 조건 알림
    }
}