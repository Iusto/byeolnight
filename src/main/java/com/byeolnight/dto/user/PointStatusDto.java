package com.byeolnight.dto.user;

import com.byeolnight.service.user.MissionService;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PointStatusDto {
    private final int currentPoints;
    private final int totalEarnedPoints;
    private final boolean todayAttended;
    private final MissionService.WeeklyAttendanceStatus weeklyMission;
    
    public static PointStatusDto of(int currentPoints, int totalEarnedPoints, boolean todayAttended, MissionService.WeeklyAttendanceStatus weeklyMission) {
        return PointStatusDto.builder()
                .currentPoints(currentPoints)
                .totalEarnedPoints(totalEarnedPoints)
                .todayAttended(todayAttended)
                .weeklyMission(weeklyMission)
                .build();
    }
}