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
public class BannedUserDto {
    private String userId;
    private String username;
    private LocalDateTime bannedUntil;
    private Long bannedBy;
    private String reason;
}
