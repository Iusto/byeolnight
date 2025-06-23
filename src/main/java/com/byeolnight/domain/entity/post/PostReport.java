package com.byeolnight.domain.entity.post;

import com.byeolnight.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostReport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 255)
    private String reason; // 사용자 입력 신고 사유

    @Column(nullable = false)
    private boolean reviewed = false; // 운영자 검토 여부

    @Column(nullable = false)
    private boolean accepted = false; // 정당한 신고로 판단됐는지 여부

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static PostReport of(User reporter, Post post, String reason) {
        return new PostReport(post, reporter, reason);
    }

    private PostReport(Post post, User user, String reason) {
        this.post = post;
        this.user = user;
        this.reason = reason;
    }
}
