package com.byeolnight.dto.user;

import com.byeolnight.domain.entity.user.PointHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PointHistoryDto {
    private Long id;
    private Integer amount;
    private String type;
    private String typeDescription;
    private String reason;
    private String referenceId;
    private LocalDateTime createdAt;
    private boolean isEarned; // 획득인지 사용인지

    public static PointHistoryDto from(PointHistory history) {
        return PointHistoryDto.builder()
                .id(history.getId())
                .amount(history.getAmount())
                .type(history.getType().name())
                .typeDescription(history.getType().getDescription())
                .reason(history.getReason())
                .referenceId(history.getReferenceId())
                .createdAt(history.getCreatedAt())
                .isEarned(history.getAmount() > 0)
                .build();
    }
}