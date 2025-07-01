package com.byeolnight.service.user;

import com.byeolnight.domain.entity.user.*;
import com.byeolnight.domain.repository.user.*;
import com.byeolnight.dto.user.PointHistoryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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

    // 포인트 상수
    private static final int DAILY_ATTENDANCE_POINTS = 10;
    private static final int POST_WRITE_POINTS = 20;
    private static final int COMMENT_WRITE_POINTS = 5;
    private static final int RECEIVE_LIKE_POINTS = 2;
    private static final int VALID_REPORT_POINTS = 10;

    /**
     * 출석 체크 포인트 지급
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean checkDailyAttendance(User user) {
        LocalDate today = LocalDate.now();
        
        try {
            // 이미 출석했는지 다시 한번 확인 (동시성 문제 방지)
            if (dailyAttendanceRepository.existsByUserAndAttendanceDate(user, today)) {
                log.debug("사용자 {}는 이미 오늘 출석했습니다.", user.getNickname());
                return false; // 이미 출석함
            }

            // 출석 기록 생성 (유니크 제약조건으로 중복 방지)
            DailyAttendance attendance = DailyAttendance.of(user, today);
            dailyAttendanceRepository.save(attendance);

            // 포인트 지급
            awardPoints(user, PointHistory.PointType.DAILY_ATTENDANCE, DAILY_ATTENDANCE_POINTS, "매일 출석 보상", null);
            
            log.info("사용자 {}에게 출석 포인트 {}점 지급", user.getNickname(), DAILY_ATTENDANCE_POINTS);
            return true;
            
        } catch (Exception e) {
            // 중복 키 예외 등이 발생한 경우 (이미 출석한 경우)
            log.warn("사용자 {}의 출석 처리 중 예외 발생: {}", user.getNickname(), e.getMessage());
            return false;
        }
    }

    /**
     * 게시글 작성 포인트 지급
     */
    @Transactional
    public void awardPostWritePoints(User user, Long postId, String content) {
        // 어뷔징 방지: 하루 최대 5개 게시글만 포인트 지급
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayPostCount = pointHistoryRepository.countByUserAndTypeAndCreatedAtAfter(
            user, PointHistory.PointType.POST_WRITE, todayStart);
            
        if (todayPostCount >= 5) {
            return; // 일일 제한 초과
        }
        
        awardPoints(user, PointHistory.PointType.POST_WRITE, POST_WRITE_POINTS, "게시글 작성 보상", postId.toString());
        log.info("사용자 {}에게 게시글 작성 포인트 {}점 지급", user.getNickname(), POST_WRITE_POINTS);
    }

    /**
     * 댓글 작성 포인트 지급
     */
    @Transactional
    public void awardCommentWritePoints(User user, Long commentId) {
        // 어뷔징 방지: 하루 최대 20개 댓글만 포인트 지급
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayCommentCount = pointHistoryRepository.countByUserAndTypeAndCreatedAtAfter(
            user, PointHistory.PointType.COMMENT_WRITE, todayStart);
            
        if (todayCommentCount >= 20) {
            return; // 일일 제한 초과
        }
        
        awardPoints(user, PointHistory.PointType.COMMENT_WRITE, COMMENT_WRITE_POINTS, "댓글 작성 보상", commentId.toString());
        log.info("사용자 {}에게 댓글 작성 포인트 {}점 지급", user.getNickname(), COMMENT_WRITE_POINTS);
    }

    /**
     * 추천 받음 포인트 지급
     */
    @Transactional
    public void awardReceiveLikePoints(User user, String referenceId) {
        awardPoints(user, PointHistory.PointType.POST_LIKED, RECEIVE_LIKE_POINTS, "게시글 추천 받음 보상", referenceId);
        log.info("사용자 {}에게 추천 받음 포인트 {}점 지급", user.getNickname(), RECEIVE_LIKE_POINTS);
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
    public void awardValidReportPoints(User user, String referenceId) {
        awardPoints(user, PointHistory.PointType.VALID_REPORT, VALID_REPORT_POINTS, "유효한 신고 인정 보상", referenceId);
        log.info("사용자 {}에게 신고 성공 포인트 {}점 지급", user.getNickname(), VALID_REPORT_POINTS);
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
     * 사용자 포인트 이력 조회 (전체)
     */
    @Transactional(readOnly = true)
    public Page<PointHistoryDto> getPointHistory(User user, Pageable pageable) {
        return pointHistoryRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(PointHistoryDto::from);
    }
    
    /**
     * 사용자 포인트 획득 이력 조회
     */
    @Transactional(readOnly = true)
    public Page<PointHistoryDto> getEarnedPointHistory(User user, Pageable pageable) {
        return pointHistoryRepository.findEarnedPointsByUser(user, pageable)
                .map(PointHistoryDto::from);
    }
    
    /**
     * 사용자 포인트 사용 이력 조회
     */
    @Transactional(readOnly = true)
    public Page<PointHistoryDto> getSpentPointHistory(User user, Pageable pageable) {
        return pointHistoryRepository.findSpentPointsByUser(user, pageable)
                .map(PointHistoryDto::from);
    }
    
    /**
     * 사용자 포인트 이력 조회 (리스트)
     */
    @Transactional(readOnly = true)
    public List<PointHistoryDto> getUserPointHistory(User user) {
        Pageable pageable = PageRequest.of(0, 50);
        return pointHistoryRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .stream()
                .map(PointHistoryDto::from)
                .collect(Collectors.toList());
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
     * 포인트 지급 공통 메서드
     */
    private void awardPoints(User user, PointHistory.PointType type, int amount, String description, String referenceId) {
        // 포인트 이력 저장
        PointHistory history = PointHistory.of(user, amount, type, description, referenceId);
        pointHistoryRepository.save(history);

        // 사용자 포인트 업데이트
        user.increasePoints(amount);
        userRepository.save(user);
    }
}