package com.byeolnight.domain.repository;

import com.byeolnight.domain.entity.Message;
import com.byeolnight.domain.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // 받은 쪽지함 (수신자 기준, 삭제되지 않은 것만)
    Page<Message> findByReceiverAndReceiverDeletedFalseOrderByCreatedAtDesc(User receiver, Pageable pageable);

    // 보낸 쪽지함 (발신자 기준, 삭제되지 않은 것만)
    Page<Message> findBySenderAndSenderDeletedFalseOrderByCreatedAtDesc(User sender, Pageable pageable);

    // 읽지 않은 쪽지 개수
    long countByReceiverAndIsReadFalseAndReceiverDeletedFalse(User receiver);

    // 특정 사용자와의 쪽지 대화 조회
    @Query("SELECT m FROM Message m WHERE " +
           "((m.sender = :user1 AND m.receiver = :user2 AND m.senderDeleted = false) OR " +
           "(m.sender = :user2 AND m.receiver = :user1 AND m.receiverDeleted = false)) " +
           "ORDER BY m.createdAt DESC")
    Page<Message> findConversationBetweenUsers(@Param("user1") User user1, @Param("user2") User user2, Pageable pageable);
}