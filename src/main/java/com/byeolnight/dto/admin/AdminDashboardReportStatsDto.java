package com.byeolnight.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminDashboardReportStatsDto {

    private final long pendingPostReports;
    private final long pendingCommentReports;
}
