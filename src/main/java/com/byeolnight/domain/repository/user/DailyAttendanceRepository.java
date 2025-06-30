package com.byeolnight.domain.repository.user;

import com.byeolnight.domain.entity.user.DailyAttendance;
import com.byeolnight.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyAttendanceRepository extends JpaRepository<DailyAttendance, Long> {

    boolean existsByUserAndAttendanceDate(User user, LocalDate date);
    
    Optional<DailyAttendance> findByUserAndAttendanceDate(User user, LocalDate date);
    
    List<DailyAttendance> findByUserAndAttendanceDateBetween(User user, LocalDate startDate, LocalDate endDate);
}