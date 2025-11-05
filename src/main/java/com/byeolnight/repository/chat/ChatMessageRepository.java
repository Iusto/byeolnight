package com.byeolnight.repository.chat;

import com.byeolnight.entity.chat.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 특정 방의 최근 메시지 조회 (최근 N개)
    @Query("SELECT c FROM ChatMessage c WHERE c.roomId = :roomId ORDER BY c.timestamp DESC")
    List<ChatMessage> findTop100ByRoomIdOrderByTimestampAsc(@Param("roomId") String roomId, Pageable pageable);

    // 블라인드된 메시지 수 조회
    long countByIsBlindedTrue();

    // 블라인드된 메시지 목록 조회
    List<ChatMessage> findByIsBlindedTrueOrderByBlindedAtDesc(Pageable pageable);

    // 활성 사용자 수 조회 (최근 1시간 내 메시지 보낸 사용자)
    @Query("SELECT COUNT(DISTINCT c.sender) FROM ChatMessage c WHERE c.timestamp >= :startTime")
    long countDistinctSenderByTimestampAfter(@Param("startTime") LocalDateTime startTime);
    
    // 특정 ID 이전 메시지 조회 (무한 스크롤용)
    List<ChatMessage> findByRoomIdAndIdLessThanOrderByTimestampDesc(String roomId, Long beforeId, Pageable pageable);
}