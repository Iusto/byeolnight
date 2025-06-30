package com.byeolnight.service.user;

import com.byeolnight.domain.entity.user.DailyAttendance;
import com.byeolnight.domain.entity.user.PointHistory;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.user.DailyAttendanceRepository;
import com.byeolnight.domain.repository.user.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class MissionService {

    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final PointService pointService;

    /**
     * 주간 출석 미션 체크 (주 5일 출석)
     */
    @Transactional
    public boolean checkWeeklyAttendanceMission(User user) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(WeekFields.of(Locale.KOREA).dayOfWeek(), 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        // 이번 주 출석 횟수 확인
        List<DailyAttendance> weeklyAttendances = dailyAttendanceRepository
                .findByUserAndAttendanceDateBetween(user, startOfWeek, endOfWeek);

        if (weeklyAttendances.size() >= 5) {
            // 이미 이번 주 미션 완료했는지 확인
            boolean alreadyCompleted = pointHistoryRepository.existsByUserAndTypeAndCreatedAtBetween(
                    user, 
                    PointHistory.PointType.MISSION_COMPLETE,
                    startOfWeek.atStartOfDay(),
                    endOfWeek.atTime(23, 59, 59)
            );

            if (!alreadyCompleted) {
                // 미션 완료 포인트 지급
                pointService.awardMissionCompletePoints(user, "주간 출석 미션 (5일 출석)");
                log.info("사용자 {}가 주간 출석 미션을 완료했습니다.", user.getNickname());
                return true;
            }
        }
        return false;
    }

    /**
     * 사용자의 이번 주 출석 현황 조회
     */
    @Transactional(readOnly = true)
    public WeeklyAttendanceStatus getWeeklyAttendanceStatus(User user) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(WeekFields.of(Locale.KOREA).dayOfWeek(), 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        List<DailyAttendance> weeklyAttendances = dailyAttendanceRepository
                .findByUserAndAttendanceDateBetween(user, startOfWeek, endOfWeek);

        boolean missionCompleted = pointHistoryRepository.existsByUserAndTypeAndCreatedAtBetween(
                user, 
                PointHistory.PointType.MISSION_COMPLETE,
                startOfWeek.atStartOfDay(),
                endOfWeek.atTime(23, 59, 59)
        );

        return WeeklyAttendanceStatus.builder()
                .attendanceDays(weeklyAttendances.size())
                .requiredDays(5)
                .missionCompleted(missionCompleted)
                .canComplete(weeklyAttendances.size() >= 5 && !missionCompleted)
                .build();
    }

    /**
     * 주간 출석 현황 DTO
     */
    public static class WeeklyAttendanceStatus {
        private final int attendanceDays;
        private final int requiredDays;
        private final boolean missionCompleted;
        private final boolean canComplete;

        public WeeklyAttendanceStatus(int attendanceDays, int requiredDays, boolean missionCompleted, boolean canComplete) {
            this.attendanceDays = attendanceDays;
            this.requiredDays = requiredDays;
            this.missionCompleted = missionCompleted;
            this.canComplete = canComplete;
        }

        public static WeeklyAttendanceStatusBuilder builder() {
            return new WeeklyAttendanceStatusBuilder();
        }

        public int getAttendanceDays() { return attendanceDays; }
        public int getRequiredDays() { return requiredDays; }
        public boolean isMissionCompleted() { return missionCompleted; }
        public boolean isCanComplete() { return canComplete; }

        public static class WeeklyAttendanceStatusBuilder {
            private int attendanceDays;
            private int requiredDays;
            private boolean missionCompleted;
            private boolean canComplete;

            public WeeklyAttendanceStatusBuilder attendanceDays(int attendanceDays) {
                this.attendanceDays = attendanceDays;
                return this;
            }

            public WeeklyAttendanceStatusBuilder requiredDays(int requiredDays) {
                this.requiredDays = requiredDays;
                return this;
            }

            public WeeklyAttendanceStatusBuilder missionCompleted(boolean missionCompleted) {
                this.missionCompleted = missionCompleted;
                return this;
            }

            public WeeklyAttendanceStatusBuilder canComplete(boolean canComplete) {
                this.canComplete = canComplete;
                return this;
            }

            public WeeklyAttendanceStatus build() {
                return new WeeklyAttendanceStatus(attendanceDays, requiredDays, missionCompleted, canComplete);
            }
        }
    }
}