package com.byeolnight.domain.entity.post;

import com.byeolnight.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    public enum Category {
        NEWS, DISCUSSION, IMAGE,
        EVENT, REVIEW
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id")
    private User writer;

    private int viewCount = 0;
    private boolean isDeleted = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean blinded = false;

    @Builder
    public Post(String title, String content, Category category, User writer) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.writer = writer;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String title, String content, Category category) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("제목은 비어 있을 수 없습니다.");
        if (content == null || content.isBlank()) throw new IllegalArgumentException("내용은 비어 있을 수 없습니다.");
        this.title = title;
        this.content = content;
        this.category = category;
    }

    public void softDelete() {
        this.isDeleted = true;
    }

    public boolean isDeleted() {
        return this.isDeleted;
    }

    public void blind() {
        this.blinded = true;
    }

}