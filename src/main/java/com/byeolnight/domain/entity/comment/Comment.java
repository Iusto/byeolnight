package com.byeolnight.domain.entity.comment;

import jakarta.persistence.*;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.entity.post.Post;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "comments",
    indexes = {
        @Index(name = "idx_comment_post_created", columnList = "post_id, createdAt"),
        @Index(name = "idx_comment_parent", columnList = "parent_id"),
        @Index(name = "idx_comment_writer", columnList = "writer_id")
    }
)
public class Comment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "writer_id", nullable = false)
    private User writer;

    // 답글 기능 활성화
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = true)
    private Comment parent;

    @Column(nullable = false, length = 500)
    private String content;
    
    @Lob
    @Column(name = "original_content", columnDefinition = "TEXT")
    private String originalContent;

    @Builder.Default
    @Column(nullable = false)
    private Boolean blinded = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean deleted = false;

    @Builder.Default
    @Column(nullable = false)
    private int likeCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private int reportCount = 0;

    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private LocalDateTime blindedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ✅ 댓글 내용 수정 메서드
    public void update(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("댓글 내용은 비어있을 수 없습니다.");
        }
        this.content = content;
    }

    // ✅ 댓글 블라인드 처리
    public void blind() {
        this.blinded = true;
        this.blindedAt = LocalDateTime.now();
    }

    // ✅ 댓글 블라인드 해제
    public void unblind() {
        this.blinded = false;
        this.blindedAt = null;
    }

    public boolean isBlinded() {
        return this.blinded;
    }

    // ✅ 댓글 소프트 삭제 (사용자용 - 원본 내용 보존)
    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        // 원본 내용 백업
        if (this.originalContent == null) {
            this.originalContent = this.content;
        }
        this.content = "이 댓글은 삭제되었습니다.";
    }


    // ✅ 댓글 복구
    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        // 원본 내용 복원
        if (this.originalContent != null) {
            this.content = this.originalContent;
            this.originalContent = null;
        }
    }


    // 좋아요 수 증가
    public void increaseLikeCount() {
        this.likeCount++;
    }

    // 좋아요 수 감소
    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    // 신고 수 증가
    public void increaseReportCount() {
        this.reportCount++;
    }


    // 신고 수 여러 개 감소
    public void decreaseReportCountBy(int count) {
        if (count <= 0) return;
        this.reportCount = Math.max(0, this.reportCount - count);
    }

    // 인기 댓글 여부 (5개 이상 좋아요)
    public boolean isPopular() {
        return this.likeCount >= 5;
    }
}

