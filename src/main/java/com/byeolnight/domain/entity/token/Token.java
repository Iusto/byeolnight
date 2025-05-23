
package com.byeolnight.domain.entity.token;

import jakarta.persistence.*;
import com.byeolnight.domain.entity.user.User;
import java.time.LocalDateTime;

@Entity
@Table(name = "tokens")
public class Token {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String refreshToken;

    private LocalDateTime expiredAt;
}
