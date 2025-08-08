package com.byeolnight.entity.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer amount; // 양수: 획득, 음수: 사용

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointType type;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "reference_id")
    private String referenceId; // 관련 게시글/댓글 ID 등

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public PointHistory(User user, Integer amount, PointType type, String reason, String referenceId) {
        this.user = user;
        this.amount = amount;
        this.type = type;
        this.reason = reason;
        this.referenceId = referenceId;
    }

    public static PointHistory of(User user, Integer amount, PointType type, String reason, String referenceId) {
        return PointHistory.builder()
                .user(user)
                .amount(amount)
                .type(type)
                .reason(reason)
                .referenceId(referenceId)
                .build();
    }

    public enum PointType {
        DAILY_ATTENDANCE("매일 출석"),
        POST_WRITE("게시글 작성"),
        COMMENT_WRITE("댓글 작성"),
        POST_LIKED("게시글 추천 받음"),
        VALID_REPORT("유효한 신고"),
        GIVE_LIKE("추천하기"),
        MISSION_COMPLETE("미션 완료"),
        ICON_PURCHASE("아이콘 구매"),
        ADMIN_AWARD("관리자 수여"),
        PENALTY("규정 위반 페널티");

        private final String description;

        PointType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}