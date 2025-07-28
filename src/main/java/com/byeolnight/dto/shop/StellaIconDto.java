package com.byeolnight.dto.shop;

import com.byeolnight.domain.entity.shop.StellaIcon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
/*
 상점 화면 (구매 가능한 아이콘 + 소유 여부)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StellaIconDto {
    private Long id;
    private String name;
    private String description;
    private String iconUrl;
    private int price;
    private String grade;
    private String gradeColor;
    private String type;
    private String animationClass;
    private boolean owned;

    public static StellaIconDto from(StellaIcon icon, boolean owned) {
        return StellaIconDto.builder()
                .id(icon.getId())
                .name(icon.getName())
                .description(icon.getDescription())
                .iconUrl(icon.getIconUrl())
                .price(icon.getPrice())
                .grade(icon.getGrade().name()) // 영어 이름 사용 (COMMON, RARE 등)
                .gradeColor(icon.getGrade().getColor())
                .type(icon.getType().getDisplayName())
                .animationClass(icon.getAnimationClass())
                .owned(owned)
                .build();
    }
}