package com.byeolnight.repository.user;

import com.byeolnight.entity.user.PointHistory;
import com.byeolnight.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    
    // 사용자별 포인트 히스토리 조회 (전체)
    Page<PointHistory> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    // 사용자별 포인트 획득 히스토리 조회
    @Query("SELECT ph FROM PointHistory ph WHERE ph.user = :user AND ph.amount > 0 ORDER BY ph.createdAt DESC")
    Page<PointHistory> findEarnedPointsByUser(@Param("user") User user, Pageable pageable);
    
    // 사용자별 포인트 사용 히스토리 조회
    @Query("SELECT ph FROM PointHistory ph WHERE ph.user = :user AND ph.amount < 0 ORDER BY ph.createdAt DESC")
    Page<PointHistory> findSpentPointsByUser(@Param("user") User user, Pageable pageable);
    
    // 특정 기간 내 특정 타입의 포인트 히스토리 개수 조회 (어뷰징 방지)
    @Query("SELECT COUNT(ph) FROM PointHistory ph WHERE ph.user = :user AND ph.type = :type AND ph.createdAt >= :since")
    long countByUserAndTypeAndCreatedAtAfter(@Param("user") User user, 
                                           @Param("type") PointHistory.PointType type, 
                                           @Param("since") LocalDateTime since);
    
    // 사용자의 총 포인트 조회
    @Query("SELECT SUM(ph.amount) FROM PointHistory ph WHERE ph.user = :user")
    Integer getTotalPointsByUser(@Param("user") User user);
    
    // 특정 기간 내 특정 타입의 포인트 히스토리 존재 여부 확인
    @Query("SELECT COUNT(ph) > 0 FROM PointHistory ph WHERE ph.user = :user AND ph.type = :type AND ph.createdAt BETWEEN :start AND :end")
    boolean existsByUserAndTypeAndCreatedAtBetween(@Param("user") User user, 
                                                 @Param("type") PointHistory.PointType type, 
                                                 @Param("start") LocalDateTime start, 
                                                 @Param("end") LocalDateTime end);
}