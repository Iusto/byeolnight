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

    // 관리자용 로그 조회 (페이징)
    Page<DeleteLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // 특정 타입별 삭제 로그 조회 (페이징)
    Page<DeleteLog> findByTargetTypeOrderByCreatedAtDesc(DeleteLog.TargetType targetType, Pageable pageable);
}