package com.byeolnight.repository.chat;

import com.byeolnight.entity.chat.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 특정 방의 최근 메시지 조회 (최신 N개를 가져온 뒤, 서비스에서 시간순 정렬)
    @Query("SELECT c FROM ChatMessage c WHERE c.roomId = :roomId ORDER BY c.id DESC")
    List<ChatMessage> findRecentByRoomIdOrderByIdDesc(@Param("roomId") String roomId, Pageable pageable);

    // 블라인드된 메시지 수 조회
    long countByIsBlindedTrue();

    // 블라인드된 메시지 목록 조회
    List<ChatMessage> findByIsBlindedTrueOrderByBlindedAtDesc(Pageable pageable);

    // 활성 사용자 수 조회 (최근 1시간 내 메시지 보낸 사용자)
    @Query("SELECT COUNT(DISTINCT c.sender) FROM ChatMessage c WHERE c.timestamp >= :startTime")
    long countDistinctSenderByTimestampAfter(@Param("startTime") LocalDateTime startTime);
    
    // 특정 ID 이전 메시지 조회 (무한 스크롤용)
    //
    // 정렬 기준은 반드시 커서와 같은 id여야 한다.
    // 이전에는 timestamp로 정렬했는데, timestamp는 @CreatedDate가 flush 전에 채우고
    // id는 INSERT 시점에 DB가 채번하므로 거의 동시에 도착한 메시지끼리 두 순서가
    // 어긋날 수 있다. 그러면 커서가 정렬 순서를 따라가지 못해 같은 메시지가
    // 다시 나오거나(중복) 영영 조회되지 않는(누락) 메시지가 생긴다.
    List<ChatMessage> findByRoomIdAndIdLessThanOrderByIdDesc(String roomId, Long beforeId, Pageable pageable);
}