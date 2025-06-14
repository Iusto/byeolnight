package com.byeolnight.domain.repository;

import com.byeolnight.domain.entity.log.AuditRefreshTokenLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRefreshTokenLogRepository extends JpaRepository<AuditRefreshTokenLog, Long> {
}
