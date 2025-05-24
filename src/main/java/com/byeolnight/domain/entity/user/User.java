package com.byeolnight.domain.entity.user;

import com.byeolnight.domain.entity.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private String phone;

    public enum Role {
        USER, ADMIN
    }
}
