package com.byeolnight.domain.entity;

import com.byeolnight.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender; // 발신자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver; // 수신자

    @Column(nullable = false, length = 100)
    private String title; // 제목

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 내용

    @Builder.Default
    @Column(nullable = false)
    private Boolean isRead = false; // 읽음 여부

    @Builder.Default
    @Column(nullable = false)
    private Boolean senderDeleted = false; // 발신자 삭제 여부

    @Builder.Default
    @Column(nullable = false)
    private Boolean receiverDeleted = false; // 수신자 삭제 여부

    @Column
    private LocalDateTime senderDeletedAt; // 발신자 삭제 시각

    @Column
    private LocalDateTime receiverDeletedAt; // 수신자 삭제 시각

    @Column
    private LocalDateTime readAt; // 읽은 시간

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 읽음 처리
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    // 발신자 삭제
    public void deleteBySender() {
        this.senderDeleted = true;
        this.senderDeletedAt = LocalDateTime.now();
    }

    // 수신자 삭제
    public void deleteByReceiver() {
        this.receiverDeleted = true;
        this.receiverDeletedAt = LocalDateTime.now();
    }

    // 양쪽 모두 삭제되었는지 확인
    public boolean isBothDeleted() {
        return senderDeleted && receiverDeleted;
    }

    // 3년 경과 후 영구 삭제 대상인지 확인
    public boolean isEligibleForPermanentDeletion() {
        if (!isBothDeleted()) return false;
        
        LocalDateTime earliestDeleteTime = senderDeletedAt.isBefore(receiverDeletedAt) 
            ? senderDeletedAt : receiverDeletedAt;
        return earliestDeleteTime.isBefore(LocalDateTime.now().minusYears(3));
    }
}