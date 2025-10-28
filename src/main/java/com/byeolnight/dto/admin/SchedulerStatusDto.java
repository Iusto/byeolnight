package com.byeolnight.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerStatusDto {
    private Integer messagesToDelete;
    private Integer postsToDelete;
    private Integer usersToCleanup;
}
