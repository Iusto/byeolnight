package com.byeolnight.entity.chat;

import com.byeolnight.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 채팅 참여 통계 엔티티
 * - 사용자별 일일 채팅 참여 현황을 추적하는 집계 테이블
 * - ChatMessage 테이블 대신 성능 최적화를 위한 별도 통계 테이블
 * - 인증서 발급 조건(별빛 채팅사) 확인에 사용
 */
@Entity
@Table(name = "chat_participations", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "participation_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatParticipation {

    /** 기본키 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 채팅 참여 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 참여 날짜 (UNIQUE 제약조건으로 하루 1개 레코드만 생성) */
    @Column(name = "participation_date", nullable = false)
    private LocalDate participationDate;

    /** 해당 날짜의 총 메시지 수 (채팅 메시지 전송 시마다 증가) */
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

    /**
     * 새로운 일일 채팅 참여 기록 생성
     * @param user 참여 사용자
     * @param date 참여 날짜
     * @return 초기 메시지 수 1로 설정된 ChatParticipation
     */
    public static ChatParticipation of(User user, LocalDate date) {
        return ChatParticipation.builder()
                .user(user)
                .participationDate(date)
                .messageCount(1)
                .build();
    }

    /**
     * 메시지 수 증가 (같은 날 추가 메시지 전송 시 호출)
     * - ChatService.trackChatParticipation()에서 사용
     */
    public void incrementMessageCount() {
        this.messageCount++;
    }
}