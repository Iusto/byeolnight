package com.byeolnight.dto.shop;

import com.byeolnight.domain.entity.shop.StellaIcon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquippedIconDto {
    private Long iconId;
    private String name;
    private String iconUrl;
    private String grade;
    private String animationClass;

    public static EquippedIconDto from(StellaIcon icon) {
        return EquippedIconDto.builder()
                .iconId(icon.getId())
                .name(icon.getName())
                .iconUrl(icon.getIconUrl())
                .grade(icon.getGrade().getDisplayName())
                .animationClass(icon.getAnimationClass())
                .build();
    }
}