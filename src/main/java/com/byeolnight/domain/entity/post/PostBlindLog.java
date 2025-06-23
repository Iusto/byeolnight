package com.byeolnight.domain.entity.post;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_blind_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostBlindLog {

    public enum Reason {
        REPORT,   // 신고 누적으로 인한 블라인드
        ADMIN     // 관리자 수동 제재
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Reason reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime blindedAt;

    private PostBlindLog(Post post, Reason reason) {
        this.post = post;
        this.reason = reason;
        this.blindedAt = LocalDateTime.now();
    }

    public static PostBlindLog of(Post post, Reason reason) {
        return new PostBlindLog(post, reason);
    }
}
