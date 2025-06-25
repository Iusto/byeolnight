package com.byeolnight.domain.repository.log;

import com.byeolnight.domain.entity.log.AuditSignupLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditSignupLogRepository extends JpaRepository<AuditSignupLog, Long> {
}
