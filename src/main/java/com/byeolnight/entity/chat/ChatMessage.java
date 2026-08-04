package com.byeolnight.entity.chat;

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
        // 채팅방 메시지 조회 + 무한 스크롤용.
        // 조회 조건(room_id)과 정렬/커서 키(id)를 그대로 담아, 원하는 위치로 바로
        // 진입한 뒤 필요한 만큼만 읽는다.
        @Index(name = "idx_chat_room_id", columnList = "room_id, id DESC"),
        // 선두 컬럼 is_blinded로 관리자 블라인드 조회를 받는다.
        // room_id/timestamp가 뒤에 있어 일반 채팅 조회에는 쓰이지 않는다.
        @Index(name = "idx_chat_room_timestamp", columnList = "is_blinded, room_id, timestamp"),
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

    @Column
    private String senderIcon;

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