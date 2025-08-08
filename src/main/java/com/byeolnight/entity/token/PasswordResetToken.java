package com.byeolnight.entity.token;

import com.byeolnight.entity.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PasswordResetToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @Column(nullable = false, unique = true)
    private String token;

    private boolean used;

    private LocalDateTime expiredAt;

    public boolean isValid() {
        return !used && expiredAt.isAfter(LocalDateTime.now());
    }

    public void markAsUsed() {
        this.used = true;
    }

    public static PasswordResetToken create(String email, String token, Duration validFor) {
        return PasswordResetToken.builder()
                .email(email)
                .token(token)
                .used(false)
                .expiredAt(LocalDateTime.now().plus(validFor))
                .build();
    }
}
