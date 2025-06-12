package com.byeolnight.domain.entity.post;

import com.byeolnight.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "post_id"}))
public class PostReport {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Post post;

    private String reason;

    private LocalDateTime reportedAt;

    public static PostReport of(User user, Post post, String reason) {
        return PostReport.builder()
                .user(user)
                .post(post)
                .reason(reason)
                .reportedAt(LocalDateTime.now())
                .build();
    }
}
