package com.byeolnight.dto.user;

import com.byeolnight.domain.entity.user.PointHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PointHistoryDto {
    private final Long id;
    private final String type;
    private final String typeDescription;
    private final int amount;
    private final String description;
    private final String referenceId;
    private final LocalDateTime createdAt;
    
    public static PointHistoryDto from(PointHistory history) {
        return PointHistoryDto.builder()
                .id(history.getId())
                .type(history.getType().name())
                .typeDescription(history.getType().getDescription())
                .amount(history.getAmount())
                .description(history.getDescription())
                .referenceId(history.getReferenceId())
                .createdAt(history.getCreatedAt())
                .build();
    }
}