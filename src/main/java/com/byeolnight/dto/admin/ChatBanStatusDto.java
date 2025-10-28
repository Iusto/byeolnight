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
public class ChatBanStatusDto {
    private boolean banned;
    private String reason;
    private LocalDateTime bannedUntil;
    private Long bannedBy;
    private Long remainingMinutes;
}
