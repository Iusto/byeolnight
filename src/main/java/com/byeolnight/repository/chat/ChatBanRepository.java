package com.byeolnight.repository.chat;

import com.byeolnight.entity.chat.ChatBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatBanRepository extends JpaRepository<ChatBan, Long> {

    // 활성 상태인 채팅 금지 조회
    List<ChatBan> findByIsActiveTrueOrderByBannedAtDesc();

    // 특정 사용자의 활성 채팅 금지 조회
    Optional<ChatBan> findByUsernameAndIsActiveTrueAndBannedUntilAfter(String username, LocalDateTime now);

    // 만료된 채팅 금지 조회
    @Query("SELECT c FROM ChatBan c WHERE c.isActive = true AND c.bannedUntil < :now")
    List<ChatBan> findExpiredBans(@Param("now") LocalDateTime now);

    // 활성 채팅 금지 수 조회
    long countByIsActiveTrueAndBannedUntilAfter(LocalDateTime now);
}