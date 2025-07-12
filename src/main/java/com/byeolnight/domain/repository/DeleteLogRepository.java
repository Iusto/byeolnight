package com.byeolnight.domain.repository;

import com.byeolnight.domain.entity.log.DeleteLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeleteLogRepository extends JpaRepository<DeleteLog, Long> {
    
    Page<DeleteLog> findByTargetTypeOrderByCreatedAtDesc(DeleteLog.TargetType targetType, Pageable pageable);
    
    Page<DeleteLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}