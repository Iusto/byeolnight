package com.byeolnight.domain.entity;

import com.byeolnight.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "suggestions")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Suggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SuggestionStatus status = SuggestionStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublic = true; // 기본값: 공개

    @Column(columnDefinition = "TEXT")
    private String adminResponse;

    @Column
    private LocalDateTime adminResponseAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 관리자 답변 추가 메서드
    public void addAdminResponse(String response, User admin, SuggestionStatus status) {
        this.adminResponse = response;
        this.admin = admin;
        this.adminResponseAt = LocalDateTime.now();
        this.status = status;
    }

    // 상태 변경 메서드
    public void updateStatus(SuggestionStatus status) {
        this.status = status;
    }

    // 건의사항 수정 메서드
    public void update(String title, String content, SuggestionCategory category) {
        update(title, content, category, this.isPublic);
    }
    
    // 건의사항 수정 메서드 (공개/비공개 포함)
    public void update(String title, String content, SuggestionCategory category, Boolean isPublic) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("제목은 필수입니다.");
        }
        if (title.length() > 100) {
            throw new IllegalArgumentException("제목은 100자를 초과할 수 없습니다.");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("내용은 필수입니다.");
        }
        if (category == null) {
            throw new IllegalArgumentException("카테고리는 필수입니다.");
        }
        
        this.title = title.trim();
        this.content = content.trim();
        this.category = category;
        if (isPublic != null) {
            this.isPublic = isPublic;
        }
    }

    public enum SuggestionCategory {
        FEATURE,    // 기능 개선
        BUG,        // 버그 신고
        UI_UX,      // UI/UX 개선
        CONTENT,    // 콘텐츠 관련
        OTHER       // 기타
    }

    public enum SuggestionStatus {
        PENDING,      // 검토 중
        IN_PROGRESS,  // 진행 중
        COMPLETED,    // 완료
        REJECTED      // 거절
    }
}