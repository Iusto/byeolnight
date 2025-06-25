package com.byeolnight.domain.repository.log;

import com.byeolnight.domain.entity.log.NicknameChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NicknameChangeHistoryRepository extends JpaRepository<NicknameChangeHistory, Long> {
}
