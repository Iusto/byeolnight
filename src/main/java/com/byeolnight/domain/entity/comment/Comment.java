
package com.byeolnight.domain.entity.comment;

import jakarta.persistence.*;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.entity.post.Post;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
public class Comment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id")
    private User writer;

    @Lob
    @Column(nullable = false)
    private String content;

    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters omitted for brevity
}
