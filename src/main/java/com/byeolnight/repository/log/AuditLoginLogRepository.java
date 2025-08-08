package com.byeolnight.repository.log;

import com.byeolnight.entity.log.AuditLoginLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLoginLogRepository extends JpaRepository<AuditLoginLog, Long> {
}
