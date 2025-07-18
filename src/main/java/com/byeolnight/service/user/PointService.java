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
    private final jakarta.persistence.EntityManager entityManager;
    private final com.byeolnight.domain.repository.shop.UserIconRepository userIconRepository;

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
        log.info("=== 출석 체크 시작 - 사용자: {}, 날짜: {} ===", user.getNickname(), today);
        
        try {
            // 엔티티 매니저 캠시 플러시 및 새로고침
            entityManager.flush();
            entityManager.clear();
            
            // 사용자 정보 새로고침
            User refreshedUser = userRepository.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
            // 이미 출석했는지 다시 한번 확인 (동시성 문제 방지)
            boolean alreadyAttended = dailyAttendanceRepository.existsByUserAndAttendanceDate(refreshedUser, today);
            log.info("출석 여부 확인 결과: {}", alreadyAttended);
            
            if (alreadyAttended) {
                log.info("사용자 {}는 이미 오늘({}) 출석했습니다.", refreshedUser.getNickname(), today);
                return false; // 이미 출석함
            }

            // 출석 기록 생성 (유니크 제약조건으로 중복 방지)
            DailyAttendance attendance = DailyAttendance.of(refreshedUser, today);
            DailyAttendance savedAttendance = dailyAttendanceRepository.save(attendance);
            log.info("출석 기록 저장 완료 - ID: {}", savedAttendance.getId());

            // 포인트 지급
            awardPoints(refreshedUser, PointHistory.PointType.DAILY_ATTENDANCE, DAILY_ATTENDANCE_POINTS, "매일 출석 보상", null);
            
            log.info("사용자 {}에게 출석 포인트 {}점 지급 완료", refreshedUser.getNickname(), DAILY_ATTENDANCE_POINTS);
            return true;
            
        } catch (Exception e) {
            // 중복 키 예외 등이 발생한 경우 (이미 출석한 경우)
            log.error("사용자 {}의 출석 처리 중 예외 발생: {}", user.getNickname(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 게시글 작성 포인트 지급
     */
    @Transactional
    public void awardPostWritePoints(User user, Long postId, String content) {
        log.info("=== 게시글 작성 포인트 지급 시작 - 사용자: {}, 게시글ID: {} ===", user.getNickname(), postId);
        
        // 어뷔징 방지: 하루 최대 5개 게시글만 포인트 지급
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayPostCount = pointHistoryRepository.countByUserAndTypeAndCreatedAtAfter(
            user, PointHistory.PointType.POST_WRITE, todayStart);
        
        log.info("오늘 게시글 작성 횟수: {}/5", todayPostCount);
            
        if (todayPostCount >= 5) {
            log.warn("사용자 {}의 일일 게시글 작성 제한 초과 ({}회)", user.getNickname(), todayPostCount);
            return; // 일일 제한 초과
        }
        
        awardPoints(user, PointHistory.PointType.POST_WRITE, POST_WRITE_POINTS, "게시글 작성 보상", postId.toString());
        log.info("사용자 {}에게 게시글 작성 포인트 {}점 지급 완료", user.getNickname(), POST_WRITE_POINTS);
    }

    /**
     * 댓글 작성 포인트 지급
     */
    @Transactional
    public void awardCommentWritePoints(User user, Long commentId) {
        log.info("=== 댓글 작성 포인트 지급 시작 - 사용자: {}, 댓글ID: {} ===", user.getNickname(), commentId);
        
        // 어뷔징 방지: 하루 최대 20개 댓글만 포인트 지급
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayCommentCount = pointHistoryRepository.countByUserAndTypeAndCreatedAtAfter(
            user, PointHistory.PointType.COMMENT_WRITE, todayStart);
        
        log.info("오늘 댓글 작성 횟수: {}/20", todayCommentCount);
            
        if (todayCommentCount >= 20) {
            log.warn("사용자 {}의 일일 댓글 작성 제한 초과 ({}회)", user.getNickname(), todayCommentCount);
            return; // 일일 제한 초과
        }
        
        awardPoints(user, PointHistory.PointType.COMMENT_WRITE, COMMENT_WRITE_POINTS, "댓글 작성 보상", commentId.toString());
        log.info("사용자 {}에게 댓글 작성 포인트 {}점 지급 완료", user.getNickname(), COMMENT_WRITE_POINTS);
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
        
        // 엔티티 매니저 캠시 플러시 및 새로고침 (읽기 전용)
        entityManager.clear();
        
        // 사용자 정보 새로고침
        User refreshedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 디버깅용: 실제 출석 기록 조회
        java.util.List<DailyAttendance> todayAttendances = dailyAttendanceRepository
                .findByUserAndAttendanceDate(refreshedUser, today);
        log.info("오늘 출석 기록 수: {}", todayAttendances.size());
        
        boolean attended = !todayAttendances.isEmpty();
        log.info("출석 여부 조회 - 사용자: {}, 날짜: {}, 출석여부: {}", refreshedUser.getNickname(), today, attended);
        return attended;
    }

    /**
     * 사용자 포인트 이력 조회 (전체)
     */
    @Transactional(readOnly = true)
    public Page<PointHistoryDto> getPointHistory(User user, Pageable pageable) {
        log.info("=== 전체 포인트 히스토리 조회 - 사용자: {} ===", user.getNickname());
        // 사용자 엔티티 새로고침
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        Page<PointHistory> histories = pointHistoryRepository.findByUserOrderByCreatedAtDesc(managedUser, pageable);
        log.info("전체 히스토리 개수: {}, 전체 요소 수: {}", histories.getContent().size(), histories.getTotalElements());
        return histories.map(PointHistoryDto::from);
    }
    
    /**
     * 사용자 포인트 획득 이력 조회
     */
    @Transactional(readOnly = true)
    public Page<PointHistoryDto> getEarnedPointHistory(User user, Pageable pageable) {
        log.info("=== 획득 포인트 히스토리 조회 - 사용자: {} ===", user.getNickname());
        // 사용자 엔티티 새로고침
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        Page<PointHistory> histories = pointHistoryRepository.findEarnedPointsByUser(managedUser, pageable);
        log.info("획득 히스토리 개수: {}, 전체 요소 수: {}", histories.getContent().size(), histories.getTotalElements());
        return histories.map(PointHistoryDto::from);
    }
    
    /**
     * 사용자 포인트 사용 이력 조회
     */
    @Transactional(readOnly = true)
    public Page<PointHistoryDto> getSpentPointHistory(User user, Pageable pageable) {
        log.info("=== 사용 포인트 히스토리 조회 - 사용자: {} ===", user.getNickname());
        // 사용자 엔티티 새로고침
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        Page<PointHistory> histories = pointHistoryRepository.findSpentPointsByUser(managedUser, pageable);
        log.info("사용 히스토리 개수: {}, 전체 요소 수: {}", histories.getContent().size(), histories.getTotalElements());
        return histories.map(PointHistoryDto::from);
    }
    
    /**
     * 사용자 포인트 이력 조회 (리스트)
     */
    @Transactional(readOnly = true)
    public List<PointHistoryDto> getUserPointHistory(User user) {
        // 사용자 엔티티 새로고침
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        Pageable pageable = PageRequest.of(0, 50);
        return pointHistoryRepository.findByUserOrderByCreatedAtDesc(managedUser, pageable)
                .stream()
                .map(PointHistoryDto::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 사용자 출석 일수 조회
     */
    @Transactional(readOnly = true)
    public int getUserAttendanceCount(User user) {
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        return (int) dailyAttendanceRepository.countByUser(managedUser);
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
     * 관리자 포인트 수여
     */
    @Transactional
    public void awardPointsByAdmin(Long userId, int points, String reason, Long adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("관리자를 찾을 수 없습니다."));
        
        log.info("관리자 포인트 수여 - 관리자: {}, 대상자: {}, 포인트: {}, 사유: {}", 
                admin.getNickname(), user.getNickname(), points, reason);
        
        String description = String.format("관리자 수여 (%s): %s", admin.getNickname(), reason);
        awardPoints(user, PointHistory.PointType.ADMIN_AWARD, points, description, adminId.toString());
        
        log.info("관리자 포인트 수여 완료 - 대상자: {}, 포인트: {}", user.getNickname(), points);
    }
    
    /**
     * 아이콘 구매 기록
     */
    @Transactional
    public void recordIconPurchase(User user, Long iconId, String iconName, int price) {
        String description = String.format("스텔라 아이콘 구매: %s", iconName);
        awardPoints(user, PointHistory.PointType.ICON_PURCHASE, -price, description, iconId.toString());
        log.info("사용자 {}의 아이콘 구매 기록 완료 - 아이콘: {}, 가격: {}", user.getNickname(), iconName, price);
    }

    /**
     * 포인트 지급 공통 메서드
     */
    @Transactional
    private void awardPoints(User user, PointHistory.PointType type, int amount, String description, String referenceId) {
        log.info("포인트 지급 처리 - 사용자: {}, 타입: {}, 금액: {}, 설명: {}", 
                user.getNickname(), type, amount, description);
        
        try {
            // 사용자 엔티티 새로고침 (영속성 컨텍스트에서 관리되도록)
            User managedUser = userRepository.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
            // 포인트 이력 저장
            PointHistory history = PointHistory.of(managedUser, amount, type, description, referenceId);
            pointHistoryRepository.save(history);
            log.info("포인트 히스토리 저장 완료 - ID: {}", history.getId());

            // 사용자 포인트 업데이트
            int beforePoints = managedUser.getPoints();
            managedUser.increasePoints(amount);
            userRepository.save(managedUser);
            log.info("사용자 포인트 업데이트 완료 - 이전: {}, 이후: {}", beforePoints, managedUser.getPoints());
            
            // 포인트 달성 인증서 체크
            try {
                com.byeolnight.service.certificate.CertificateService certificateService = 
                    com.byeolnight.infrastructure.config.ApplicationContextProvider
                        .getBean(com.byeolnight.service.certificate.CertificateService.class);
                certificateService.checkAndIssueCertificates(managedUser, 
                    com.byeolnight.service.certificate.CertificateService.CertificateCheckType.POINT_ACHIEVEMENT);
            } catch (Exception certError) {
                log.error("포인트 달성 인증서 체크 실패: {}", certError.getMessage());
            }
            
        } catch (Exception e) {
            log.error("포인트 지급 처리 중 오류 발생 - 사용자: {}, 오류: {}", user.getNickname(), e.getMessage(), e);
            throw e;
        }
    }
}