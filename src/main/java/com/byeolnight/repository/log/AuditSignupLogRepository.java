package com.byeolnight.repository.log;

import com.byeolnight.entity.log.AuditSignupLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditSignupLogRepository extends JpaRepository<AuditSignupLog, Long> {
}
