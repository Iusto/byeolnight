package com.byeolnight.domain.entity.user;

import com.byeolnight.domain.entity.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "point_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PointHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointType type;

    @Column(nullable = false)
    private int amount; // 양수: 획득, 음수: 차감

    @Column(length = 500)
    private String description;

    @Column
    private String referenceId; // 관련 게시글/댓글 ID 등

    public enum PointType {
        DAILY_LOGIN("매일 출석", 10),
        POST_WRITE("게시글 작성", 20),
        COMMENT_WRITE("댓글 작성", 5),
        RECEIVE_LIKE("추천 받음", 2),
        GIVE_LIKE("추천하기", 1),
        REPORT_SUCCESS("신고 성공", 10),
        MISSION_COMPLETE("미션 완료", 50),
        PENALTY("규정 위반", -10);

        private final String description;
        private final int defaultAmount;

        PointType(String description, int defaultAmount) {
            this.description = description;
            this.defaultAmount = defaultAmount;
        }

        public String getDescription() { return description; }
        public int getDefaultAmount() { return defaultAmount; }
    }

    public static PointHistory create(User user, PointType type, int amount, String description, String referenceId) {
        return PointHistory.builder()
                .user(user)
                .type(type)
                .amount(amount)
                .description(description)
                .referenceId(referenceId)
                .build();
    }
}