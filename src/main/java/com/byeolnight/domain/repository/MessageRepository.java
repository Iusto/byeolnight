package com.byeolnight.domain.repository;

import com.byeolnight.domain.entity.Message;
import com.byeolnight.domain.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {
    
    // 받은 쪽지 목록
    Page<Message> findByReceiverOrderByCreatedAtDesc(User receiver, Pageable pageable);
    
    // 보낸 쪽지 목록
    Page<Message> findBySenderOrderByCreatedAtDesc(User sender, Pageable pageable);
    
    // 읽지 않은 쪽지 개수
    long countByReceiverAndIsReadFalse(User receiver);
    
    // 특정 사용자와의 쪽지 대화
    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender = :user1 AND m.receiver = :user2) OR " +
           "(m.sender = :user2 AND m.receiver = :user1) " +
           "ORDER BY m.createdAt DESC")
    Page<Message> findConversation(@Param("user1") User user1, 
                                  @Param("user2") User user2, 
                                  Pageable pageable);
}