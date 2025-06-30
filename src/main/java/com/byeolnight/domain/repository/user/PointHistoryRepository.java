package com.byeolnight.domain.repository.user;

import com.byeolnight.domain.entity.user.PointHistory;
import com.byeolnight.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    List<PointHistory> findByUserOrderByCreatedAtDesc(User user);

    @Query("SELECT COUNT(p) FROM PointHistory p WHERE p.user = :user AND p.type = :type AND p.createdAt >= :since")
    long countByUserAndTypeAndCreatedAtAfter(@Param("user") User user, 
                                           @Param("type") PointHistory.PointType type, 
                                           @Param("since") LocalDateTime since);

    @Query("SELECT SUM(p.amount) FROM PointHistory p WHERE p.user = :user")
    Integer getTotalPointsByUser(@Param("user") User user);
    
    boolean existsByUserAndTypeAndCreatedAtBetween(User user, PointHistory.PointType type, LocalDateTime startDate, LocalDateTime endDate);
}