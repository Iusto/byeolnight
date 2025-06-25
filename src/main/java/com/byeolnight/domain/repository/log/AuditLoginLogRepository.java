package com.byeolnight.domain.repository.log;

import com.byeolnight.domain.entity.log.AuditLoginLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLoginLogRepository extends JpaRepository<AuditLoginLog, Long> {
}
