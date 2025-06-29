package com.byeolnight.dto.shop;

import com.byeolnight.domain.entity.shop.UserIcon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserIconDto {
    private Long id;
    private Long iconId;
    private String name;
    private String description;
    private String iconUrl;
    private String grade;
    private String gradeColor;
    private String type;
    private String animationClass;
    private int purchasePrice;
    private boolean equipped;
    private LocalDateTime purchasedAt;

    public static UserIconDto from(UserIcon userIcon) {
        return UserIconDto.builder()
                .id(userIcon.getId())
                .iconId(userIcon.getStellaIcon().getId())
                .name(userIcon.getStellaIcon().getName())
                .description(userIcon.getStellaIcon().getDescription())
                .iconUrl(userIcon.getStellaIcon().getIconUrl())
                .grade(userIcon.getStellaIcon().getGrade().getDisplayName())
                .gradeColor(userIcon.getStellaIcon().getGrade().getColor())
                .type(userIcon.getStellaIcon().getType().getDisplayName())
                .animationClass(userIcon.getStellaIcon().getAnimationClass())
                .purchasePrice(userIcon.getPurchasePrice())
                .equipped(userIcon.isEquipped())
                .purchasedAt(userIcon.getCreatedAt())
                .build();
    }
}