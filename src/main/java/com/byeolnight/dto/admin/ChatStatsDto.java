package com.byeolnight.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatStatsDto {
    private long totalMessages;
    private long blindedMessages;
    private long bannedUsers;
    private long activeUsers;
}