package com.byeolnight.domain.entity.post;

import com.byeolnight.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    public enum Category {
        NEWS, DISCUSSION, IMAGE, EVENT, REVIEW, FREE, NOTICE, SUGGESTION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
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
    private boolean isDeleted = false;

    @Column(nullable = false)
    private boolean blinded = false;

    @CreationTimestamp  // 생성 시 자동으로 현재 시간 설정
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp    // 수정 시 자동으로 현재 시간 설정
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
    }

    public void blind() {
        this.blinded = true;
    }

    public void unblind() {
        this.blinded = false;
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
