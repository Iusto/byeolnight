package com.byeolnight.domain.entity.comment;

import com.byeolnight.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comment_reports",
       uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 255)
    private String reason; // 사용자 입력 신고 사유

    @Column(length = 500)
    private String description; // 상세 설명

    @Column(nullable = false)
    private boolean reviewed = false; // 운영자 검토 여부

    @Column(nullable = false)
    private boolean accepted = false; // 정당한 신고로 판단됐는지 여부
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy; // 처리한 관리자
    
    @Column
    private LocalDateTime processedAt; // 처리 시각
    
    @Column(length = 500)
    private String rejectReason; // 거부 사유

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static CommentReport of(User reporter, Comment comment, String reason, String description) {
        return new CommentReport(comment, reporter, reason, description);
    }

    private CommentReport(Comment comment, User user, String reason, String description) {
        this.comment = comment;
        this.user = user;
        this.reason = reason;
        this.description = description;
    }
    
    /**
     * 신고 처리 여부 확인
     */
    public boolean isProcessed() {
        return this.reviewed;
    }
    
    /**
     * 신고 승인 여부 확인
     */
    public boolean isAccepted() {
        return this.accepted;
    }
    
    /**
     * 신고 검토 여부 확인
     */
    public boolean isReviewed() {
        return this.reviewed;
    }
    
    /**
     * 신고 승인 처리
     */
    public void approve(User admin) {
        this.reviewed = true;
        this.accepted = true;
        this.processedBy = admin;
        this.processedAt = LocalDateTime.now();
    }
    
    /**
     * 신고 거부 처리
     */
    public void reject(User admin, String rejectReason) {
        this.reviewed = true;
        this.accepted = false;
        this.processedBy = admin;
        this.processedAt = LocalDateTime.now();
        this.rejectReason = rejectReason;
    }
}