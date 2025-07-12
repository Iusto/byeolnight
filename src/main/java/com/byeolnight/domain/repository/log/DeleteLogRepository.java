package com.byeolnight.domain.repository.log;

import com.byeolnight.domain.entity.log.DeleteLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DeleteLogRepository extends JpaRepository<DeleteLog, Long> {
    
    // 특정 대상의 삭제 로그 조회
    List<DeleteLog> findByTargetIdAndTargetTypeOrderByCreatedAtDesc(Long targetId, DeleteLog.TargetType targetType);
    
    // 관리자용 로그 조회 (페이징)
    Page<DeleteLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // 특정 타입별 삭제 로그 조회 (페이징)
    Page<DeleteLog> findByTargetTypeOrderByCreatedAtDesc(DeleteLog.TargetType targetType, Pageable pageable);
    
    // 특정 기간 내 삭제 로그 조회
    @Query("SELECT d FROM DeleteLog d WHERE d.createdAt BETWEEN :startDate AND :endDate ORDER BY d.createdAt DESC")
    List<DeleteLog> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}