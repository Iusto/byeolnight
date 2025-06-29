package com.byeolnight.domain.entity.shop;

import com.byeolnight.domain.entity.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stella_icons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StellaIcon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // 아이콘 이름

    @Column(nullable = false)
    private String description; // 아이콘 설명

    @Column(nullable = false)
    private String iconUrl; // 아이콘 이미지 URL

    @Column(nullable = false)
    private int price; // 포인트 가격

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IconGrade grade; // 아이콘 등급

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IconType type; // 아이콘 타입

    @Column(nullable = false)
    @Builder.Default
    private boolean available = true; // 구매 가능 여부

    @Column
    private String animationClass; // CSS 애니메이션 클래스

    public enum IconGrade {
        COMMON("일반", "#9CA3AF"),
        RARE("희귀", "#3B82F6"), 
        LEGENDARY("전설", "#F59E0B"),
        EVENT("한정", "#EF4444");

        private final String displayName;
        private final String color;

        IconGrade(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() { return displayName; }
        public String getColor() { return color; }
    }

    public enum IconType {
        STATIC("정적"), // PNG/SVG 고정 이미지
        ANIMATED("애니메이션"), // CSS 애니메이션 적용
        SPECIAL("특수"); // 특수 효과

        private final String displayName;

        IconType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }

    // 아이콘 비활성화
    public void disable() {
        this.available = false;
    }

    // 아이콘 활성화
    public void enable() {
        this.available = true;
    }
}