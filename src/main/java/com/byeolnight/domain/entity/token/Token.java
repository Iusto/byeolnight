
package com.byeolnight.domain.entity.token;

import jakarta.persistence.*;
import com.byeolnight.domain.entity.user.User;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
public class Token {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private User user;

    @Column(nullable = false)
    private String refreshToken;

    private boolean expired;

    public void expire() {
        this.expired = true;
    }
}

