package com.byeolnight.repository.log;

import com.byeolnight.entity.log.NicknameChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NicknameChangeHistoryRepository extends JpaRepository<NicknameChangeHistory, Long> {
}
