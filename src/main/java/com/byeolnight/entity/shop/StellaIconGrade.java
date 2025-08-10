package com.byeolnight.entity.shop;

/**
 * 스텔라 아이콘 등급 열거형
 */
public enum StellaIconGrade {
    FREE("무료", "#6B7280"),
    COMMON("일반", "#9CA3AF"),
    RARE("희귀", "#3B82F6"),
    EPIC("영웅", "#8B5CF6"),
    LEGENDARY("전설", "#F59E0B"),
    MYTHIC("신화", "#EF4444");

    private final String displayName;
    private final String color;

    StellaIconGrade(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }
}