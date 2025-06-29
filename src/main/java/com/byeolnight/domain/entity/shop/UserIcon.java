package com.byeolnight.domain.entity.shop;

import com.byeolnight.domain.entity.common.BaseTimeEntity;
import com.byeolnight.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_icons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserIcon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stella_icon_id", nullable = false)
    private StellaIcon stellaIcon;

    @Column(nullable = false)
    private int purchasePrice; // 구매 당시 가격

    @Column(nullable = false)
    @Builder.Default
    private boolean equipped = false; // 장착 여부

    public static UserIcon of(User user, StellaIcon stellaIcon, int purchasePrice) {
        return UserIcon.builder()
                .user(user)
                .stellaIcon(stellaIcon)
                .purchasePrice(purchasePrice)
                .build();
    }

    // 아이콘 장착
    public void equip() {
        this.equipped = true;
    }

    // 아이콘 해제
    public void unequip() {
        this.equipped = false;
    }
}