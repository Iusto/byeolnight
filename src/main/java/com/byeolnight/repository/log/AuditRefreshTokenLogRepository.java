package com.byeolnight.repository.log;

import com.byeolnight.entity.log.AuditRefreshTokenLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRefreshTokenLogRepository extends JpaRepository<AuditRefreshTokenLog, Long> {
}
