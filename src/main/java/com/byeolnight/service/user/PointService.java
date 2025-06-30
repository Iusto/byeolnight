package com.byeolnight.service.user;

import com.byeolnight.domain.entity.user.*;
import com.byeolnight.domain.repository.user.*;
import com.byeolnight.dto.user.PointHistoryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointService {

    private final PointHistoryRepository pointHistoryRepository;
    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final UserRepository userRepository;

    /**
     * 출석 체크 포인트 지급
     */
    @Transactional
    public boolean checkDailyAttendance(User user) {
        LocalDate today = LocalDate.now();
        
        // 이미 출석했는지 확인
        if (dailyAttendanceRepository.existsByUserAndAttendanceDate(user, today)) {
            return false; // 이미 출석함
        }

        // 출석 기록 생성
        DailyAttendance attendance = DailyAttendance.create(user, today, 10);
        dailyAttendanceRepository.save(attendance);

        // 포인트 지급
        awardPoints(user, PointHistory.PointType.DAILY_LOGIN, 10, "일일 출석 보상", null);
        
        log.info("사용자 {}에게 출석 포인트 10점 지급", user.getNickname());
        return true;
    }

    /**
     * 게시글 작성 포인트 지급
     */
    @Transactional
    public void awardPostWritePoints(User user, Long postId, String content) {
        if (content.length() >= 200) {
            awardPoints(user, PointHistory.PointType.POST_WRITE, 20, "게시글 작성 보상", postId.toString());
            log.info("사용자 {}에게 게시글 작성 포인트 20점 지급", user.getNickname());
        }
    }

    /**
     * 댓글 작성 포인트 지급
     */
    @Transactional
    public void awardCommentWritePoints(User user, Long commentId, String content) {
        if (content.length() >= 30) {
            awardPoints(user, PointHistory.PointType.COMMENT_WRITE, 5, "댓글 작성 보상", commentId.toString());
            log.info("사용자 {}에게 댓글 작성 포인트 5점 지급", user.getNickname());
        }
    }

    /**
     * 추천 받음 포인트 지급
     */
    @Transactional
    public void awardReceiveLikePoints(User user, String referenceId) {
        awardPoints(user, PointHistory.PointType.RECEIVE_LIKE, 2, "추천 받음 보상", referenceId);
        log.info("사용자 {}에게 추천 받음 포인트 2점 지급", user.getNickname());
    }

    /**
     * 추천하기 포인트 지급 (일일 제한)
     */
    @Transactional
    public boolean awardGiveLikePoints(User user, String referenceId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayLikeCount = pointHistoryRepository.countByUserAndTypeAndCreatedAtAfter(
            user, PointHistory.PointType.GIVE_LIKE, todayStart);

        if (todayLikeCount >= 10) {
            return false; // 일일 제한 초과
        }

        awardPoints(user, PointHistory.PointType.GIVE_LIKE, 1, "추천하기 보상", referenceId);
        log.info("사용자 {}에게 추천하기 포인트 1점 지급", user.getNickname());
        return true;
    }

    /**
     * 신고 성공 포인트 지급
     */
    @Transactional
    public void awardReportSuccessPoints(User user, String referenceId) {
        awardPoints(user, PointHistory.PointType.REPORT_SUCCESS, 10, "신고 성공 보상", referenceId);
        log.info("사용자 {}에게 신고 성공 포인트 10점 지급", user.getNickname());
    }

    /**
     * 규정 위반 페널티
     */
    @Transactional
    public void applyPenalty(User user, String reason, String referenceId) {
        awardPoints(user, PointHistory.PointType.PENALTY, -10, "규정 위반 페널티: " + reason, referenceId);
        log.info("사용자 {}에게 규정 위반 페널티 -10점 적용", user.getNickname());
    }

    /**
     * 미션 완료 포인트 지급
     */
    @Transactional
    public void awardMissionCompletePoints(User user, String missionDescription) {
        awardPoints(user, PointHistory.PointType.MISSION_COMPLETE, 50, "미션 완료: " + missionDescription, null);
        log.info("사용자 {}에게 미션 완료 포인트 50점 지급", user.getNickname());
    }

    /**
     * 사용자의 총 포인트 조회
     */
    @Transactional(readOnly = true)
    public int getUserTotalPoints(User user) {
        Integer totalPoints = pointHistoryRepository.getTotalPointsByUser(user);
        return totalPoints != null ? totalPoints : 0;
    }

    /**
     * 오늘 출석 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean isTodayAttended(User user) {
        LocalDate today = LocalDate.now();
        return dailyAttendanceRepository.existsByUserAndAttendanceDate(user, today);
    }

    /**
     * 사용자 포인트 이력 조회
     */
    @Transactional(readOnly = true)
    public List<PointHistoryDto> getUserPointHistory(User user) {
        return pointHistoryRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(PointHistoryDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 포인트 지급 공통 메서드
     */
    private void awardPoints(User user, PointHistory.PointType type, int amount, String description, String referenceId) {
        // 포인트 이력 저장
        PointHistory history = PointHistory.create(user, type, amount, description, referenceId);
        pointHistoryRepository.save(history);

        // 사용자 포인트 업데이트
        user.increasePoints(amount);
        userRepository.save(user);
    }
}