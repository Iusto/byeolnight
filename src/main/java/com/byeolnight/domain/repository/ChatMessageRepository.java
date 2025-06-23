package com.byeolnight.domain.repository;

import com.byeolnight.domain.entity.chat.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findTop100ByRoomIdOrderByTimestampAsc(String roomId);
}
