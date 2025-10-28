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
public class CinemaStatusDto {
    private Long totalCinemaPosts;
    private Boolean latestPostExists;
    private String latestPostTitle;
    private LocalDateTime lastUpdated;
    private Long daysSinceLastUpdate;
    private Boolean systemHealthy;
    private String warning;
    private Long todayPosts;
    private Boolean googleApiConfigured;
    private Boolean openaiApiConfigured;
    private SystemConfigDto systemConfig;
    private String statusMessage;
    private String error;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemConfigDto {
        private Boolean schedulerEnabled;
        private String dailyScheduleTime;
        private String retryTimes;
        private Integer maxRetryCount;
        private Integer keywordCount;
    }
}
