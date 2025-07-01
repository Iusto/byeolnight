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
@Table(name = "comments")
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

    @Lob
    @Column(nullable = false)
    private String content;

    @Builder.Default
    @Column(nullable = false)
    private Boolean blinded = false;

    private LocalDateTime createdAt;

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
    }

    // ✅ 댓글 블라인드 해제
    public void unblind() {
        this.blinded = false;
    }
}

