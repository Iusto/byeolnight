package com.byeolnight.domain.entity.post;

import com.byeolnight.domain.entity.user.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "posts",
    indexes = {
        @Index(name = "idx_post_created_at", columnList = "created_at"),
        @Index(name = "idx_post_category_created", columnList = "category, created_at"),
        @Index(name = "idx_post_writer_created", columnList = "writer_id, created_at"),
        @Index(name = "idx_post_pinned_created", columnList = "pinned, created_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    public enum Category {
        NEWS, DISCUSSION, IMAGE, REVIEW, FREE, NOTICE, SUGGESTION, STARLIGHT_CINEMA
    }

    public enum BlindType {
        ADMIN_BLIND,    // 관리자 직접 블라인드
        REPORT_BLIND    // 신고로 인한 자동 블라인드
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 10000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "writer_id", nullable = false)
    private User writer;

    @Column(nullable = false)
    private int viewCount = 0;

    @Column(nullable = false)
    private int likeCount = 0;

    @Column(nullable = false)
    private int reportCount = 0;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @Column(nullable = false)
    private boolean blinded = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "blind_type")
    private BlindType blindType;

    @Column(name = "blinded_by_admin_id")
    private Long blindedByAdminId;

    @Column(nullable = false)
    private boolean pinned = false; // 상단 고정 여부

    @Column(nullable = false)
    private boolean discussionTopic = false; // AI 토론 주제 여부

    @Column
    private Long originTopicId; // 원본 토론 주제 ID (의견글인 경우)

    @CreationTimestamp  // 생성 시 자동으로 현재 시간 설정
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;

    @UpdateTimestamp    // 수정 시 자동으로 현재 시간 설정
    @Column(name = "updated_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @Column(name = "blinded_at")
    private LocalDateTime blindedAt;

    @Builder
    public Post(String title, String content, Category category, User writer) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.writer = writer;
    }

    public void update(String title, String content, Category category) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("제목은 비어 있을 수 없습니다.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("내용은 비어 있을 수 없습니다.");
        }
        this.title = title;
        this.content = content;
        this.category = category;
    }

    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }

    public void blind() {
        this.blinded = true;
        this.blindedAt = LocalDateTime.now();
        this.blindType = BlindType.REPORT_BLIND; // 기본값
    }

    public void blindByAdmin(Long adminId) {
        this.blinded = true;
        this.blindedAt = LocalDateTime.now();
        this.blindType = BlindType.ADMIN_BLIND;
        this.blindedByAdminId = adminId;
    }

    public void unblind() {
        this.blinded = false;
        this.blindedAt = null;
        this.blindType = null;
        this.blindedByAdminId = null;
    }

    public boolean isDeleted() {
        return this.isDeleted;
    }

    public boolean isBlinded() {
        return this.blinded;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void increaseReportCount() {
        this.reportCount++;
    }

    public void decreaseReportCount() {
        if (this.reportCount > 0) {
            this.reportCount--;
        }
    }

    public void pin() {
        this.pinned = true;
    }

    public void unpin() {
        this.pinned = false;
    }

    public void setAsDiscussionTopic() {
        this.discussionTopic = true;
        this.pinned = true;
    }
    
    public void setOriginTopicId(Long originTopicId) {
        this.originTopicId = originTopicId;
    }

    public enum SortType {
        RECENT("recent"),
        POPULAR("popular");

        private final String value;

        SortType(String value) {
            this.value = value;
        }

        public static SortType from(String value) {
            if (value == null || value.isBlank()) return RECENT;
            for (SortType sort : values()) {
                if (sort.value.equalsIgnoreCase(value)) return sort;
            }
            throw new IllegalArgumentException("지원하지 않는 정렬 방식입니다.");
        }

        public String getValue() {
            return value;
        }
    }
}
