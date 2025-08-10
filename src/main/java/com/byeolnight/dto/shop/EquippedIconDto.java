package com.byeolnight.dto.shop;

import lombok.Builder;
import lombok.Getter;
/*
 프로필 화면 (장착된 아이콘 간단 정보)
 */
@Getter
public class EquippedIconDto {
    private final Long iconId;
    private final String iconName;
    private final String iconUrl;

    @Builder
    public EquippedIconDto(Long iconId, String iconName, String iconUrl) {
        this.iconId = iconId;
        this.iconName = iconName;
        this.iconUrl = iconUrl;
    }
}