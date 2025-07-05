package com.byeolnight.dto.shop;

import lombok.Builder;
import lombok.Getter;

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