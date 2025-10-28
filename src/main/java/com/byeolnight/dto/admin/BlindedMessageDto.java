package com.byeolnight.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlindedMessageDto {
    private String messageId;
    private Long blindedBy;
    private LocalDateTime blindedAt;
    private String originalMessage;
    private String sender;
}
