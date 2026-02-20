package com.byeolnight.service.user;

import com.byeolnight.dto.user.PointHistoryDto;
import com.byeolnight.entity.user.DailyAttendance;
import com.byeolnight.entity.user.PointHistory;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.exception.NotFoundException;
import com.byeolnight.repository.user.DailyAttendanceRepository;
import com.byeolnight.repository.user.PointHistoryRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.service.certificate.CertificateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final CertificateService certificateService;

    private static final int DAILY_ATTENDANCE_POINTS = 10;
    private static final int POST_WRITE_POINTS = 20;
    private static final int COMMENT_WRITE_POINTS = 5;
    private static final int RECEIVE_LIKE_POINTS = 2;
    private static final int VALID_REPORT_POINTS = 10;

    @Transactional
    public boolean checkDailyAttendance(User user) {
        LocalDate today = LocalDate.now();
        try {
            DailyAttendance attendance = DailyAttendance.of(user, today);
            dailyAttendanceRepository.save(attendance);
            awardPoints(user, PointHistory.PointType.DAILY_ATTENDANCE, DAILY_ATTENDANCE_POINTS, "매일 출석 보상", null);
            log.info("출석 포인트 지급 - 사용자: {}, 포인트: {}", user.getNickname(), DAILY_ATTENDANCE_POINTS);
            return true;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.debug("이미 출석한 사용자: {}, 날짜: {}", user.getNickname(), today);
            return false;
        } catch (Exception e) {
            log.error("출석 처리 중 오류 - 사용자: {}: {}", user.getNickname(), e.getMessage(), e);
            return false;
        }
    }

    @Transactional
    public void awardPostWritePoints(User user, Long postId, String content) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayPostCount = pointHistoryRepository.countByUserAndTypeAndCreatedAtAfter(
            user, PointHistory.PointType.POST_WRITE, todayStart);

        if (todayPostCount >= 5) {
            log.debug("일일 게시글 포인트 한도 초과 - 사용자: {} ({}회)", user.getNickname(), todayPostCount);
            return;
        }

        awardPoints(user, PointHistory.PointType.POST_WRITE, POST_WRITE_POINTS, "게시글 작성 보상", postId.toString());
        log.info("게시글 작성 포인트 지급 - 사용자: {}, 포인트: {}", user.getNickname(), POST_WRITE_POINTS);
    }

    @Transactional
    public void awardCommentWritePoints(User user, Long commentId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayCommentCount = pointHistoryRepository.countByUserAndTypeAndCreatedAtAfter(
            user, PointHistory.PointType.COMMENT_WRITE, todayStart);

        if (todayCommentCount >= 20) {
            log.debug("일일 댓글 포인트 한도 초과 - 사용자: {} ({}회)", user.getNickname(), todayCommentCount);
            return;
        }

        awardPoints(user, PointHistory.PointType.COMMENT_WRITE, COMMENT_WRITE_POINTS, "댓글 작성 보상", commentId.toString());
        log.info("댓글 작성 포인트 지급 - 사용자: {}, 포인트: {}", user.getNickname(), COMMENT_WRITE_POINTS);
    }

    @Transactional
    public void awardReceiveLikePoints(User user, String referenceId) {
        awardPoints(user, PointHistory.PointType.POST_LIKED, RECEIVE_LIKE_POINTS, "게시글 추천 받음 보상", referenceId);
    }

    @Transactional
    public boolean awardGiveLikePoints(User user, String referenceId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayLikeCount = pointHistoryRepository.countByUserAndTypeAndCreatedAtAfter(
            user, PointHistory.PointType.GIVE_LIKE, todayStart);

        if (todayLikeCount >= 10) {
            return false;
        }

        awardPoints(user, PointHistory.PointType.GIVE_LIKE, 1, "추천하기 보상", referenceId);
        return true;
    }

    @Transactional
    public void awardValidReportPoints(User user, String referenceId) {
        awardPoints(user, PointHistory.PointType.VALID_REPORT, VALID_REPORT_POINTS, "유효한 신고 인정 보상", referenceId);
    }

    @Transactional
    public void applyPenalty(User user, String reason, String referenceId) {
        awardPointsWithPenalty(user, PointHistory.PointType.PENALTY, -10, "규정 위반 페널티: " + reason, referenceId);
        log.info("페널티 적용 - 사용자: {}, 사유: {}", user.getNickname(), reason);
    }

    @Transactional(readOnly = true)
    public int getUserTotalPoints(User user) {
        Integer totalPoints = pointHistoryRepository.getTotalPointsByUser(user);
        return totalPoints != null ? totalPoints : 0;
    }

    @Transactional(readOnly = true)
    public boolean isTodayAttended(User user) {
        return dailyAttendanceRepository.existsByUserAndAttendanceDate(user, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public Page<PointHistoryDto> getPointHistory(User user, Pageable pageable) {
        return pointHistoryRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(PointHistoryDto::from);
    }

    @Transactional(readOnly = true)
    public Page<PointHistoryDto> getEarnedPointHistory(User user, Pageable pageable) {
        return pointHistoryRepository.findEarnedPointsByUser(user, pageable)
                .map(PointHistoryDto::from);
    }

    @Transactional(readOnly = true)
    public Page<PointHistoryDto> getSpentPointHistory(User user, Pageable pageable) {
        return pointHistoryRepository.findSpentPointsByUser(user, pageable)
                .map(PointHistoryDto::from);
    }

    @Transactional(readOnly = true)
    public List<PointHistoryDto> getUserPointHistory(User user) {
        Pageable pageable = PageRequest.of(0, 50);
        return pointHistoryRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .stream()
                .map(PointHistoryDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public int getUserAttendanceCount(User user) {
        return (int) dailyAttendanceRepository.countByUser(user);
    }

    @Transactional
    public void awardMissionCompletePoints(User user, String missionDescription) {
        awardPoints(user, PointHistory.PointType.MISSION_COMPLETE, 50, "미션 완료: " + missionDescription, null);
        log.info("미션 완료 포인트 지급 - 사용자: {}", user.getNickname());
    }

    @Transactional
    public void awardPointsByAdmin(Long userId, int points, String reason, Long adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("관리자를 찾을 수 없습니다."));

        String description = String.format("관리자 수여 (%s): %s", admin.getNickname(), reason);
        awardPoints(user, PointHistory.PointType.ADMIN_AWARD, points, description, adminId.toString());
        log.info("관리자 포인트 수여 - 관리자: {}, 대상: {}, 포인트: {}", admin.getNickname(), user.getNickname(), points);
    }

    @Transactional
    public void recordIconPurchase(User user, Long iconId, String iconName, int price) {
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        if (managedUser.getPoints() < price) {
            throw new IllegalArgumentException("포인트가 부족합니다. 필요: " + price + ", 보유: " + managedUser.getPoints());
        }

        String description = String.format("스텔라 아이콘 구매: %s", iconName);
        awardPoints(managedUser, PointHistory.PointType.ICON_PURCHASE, -price, description, iconId.toString());
        log.info("아이콘 구매 - 사용자: {}, 아이콘: {}, 가격: {}", managedUser.getNickname(), iconName, price);
    }

    private void awardPointsWithPenalty(User user, PointHistory.PointType type, int amount, String description, String referenceId) {
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        pointHistoryRepository.save(PointHistory.of(managedUser, amount, type, description, referenceId));

        if (amount < 0) {
            int penaltyAmount = -amount;
            if (managedUser.getPoints() >= penaltyAmount) {
                managedUser.decreasePoints(penaltyAmount);
            } else {
                managedUser.increasePoints(-managedUser.getPoints());
            }
        } else {
            managedUser.increasePoints(amount);
        }
        userRepository.save(managedUser);
    }

    private void awardPoints(User user, PointHistory.PointType type, int amount, String description, String referenceId) {
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        pointHistoryRepository.save(PointHistory.of(managedUser, amount, type, description, referenceId));

        if (amount >= 0) {
            managedUser.increasePoints(amount);
        } else {
            managedUser.decreasePoints(-amount);
        }
        userRepository.save(managedUser);

        try {
            certificateService.checkAndIssueCertificates(managedUser,
                CertificateService.CertificateCheckType.POINT_ACHIEVEMENT);
        } catch (Exception certError) {
            log.error("포인트 달성 인증서 체크 실패: {}", certError.getMessage());
        }
    }
}
