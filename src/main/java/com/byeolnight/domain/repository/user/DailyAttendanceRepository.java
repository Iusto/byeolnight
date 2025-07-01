package com.byeolnight.domain.repository.user;

import com.byeolnight.domain.entity.user.DailyAttendance;
import com.byeolnight.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyAttendanceRepository extends JpaRepository<DailyAttendance, Long> {
    
    // 특정 날짜에 출석했는지 확인
    boolean existsByUserAndAttendanceDate(User user, LocalDate date);
    
    // 사용자의 총 출석 일수
    long countByUser(User user);
    
    // 특정 기간 내 출석 기록 조회
    List<DailyAttendance> findByUserAndAttendanceDateBetween(User user, LocalDate startDate, LocalDate endDate);
}