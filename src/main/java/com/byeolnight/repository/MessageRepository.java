package com.byeolnight.repository;

import com.byeolnight.entity.Message;
import com.byeolnight.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // 받은 쪽지함 (수신자 기준, 삭제되지 않은 것만)
    Page<Message> findByReceiverAndReceiverDeletedFalseOrderByCreatedAtDesc(User receiver, Pageable pageable);

    // 보낸 쪽지함 (발신자 기준, 삭제되지 않은 것만)
    Page<Message> findBySenderAndSenderDeletedFalseOrderByCreatedAtDesc(User sender, Pageable pageable);

    // 읽지 않은 쪽지 개수
    long countByReceiverAndIsReadFalseAndReceiverDeletedFalse(User receiver);

    // 3년 경과 후 영구 삭제 대상 쪽지 조회
    @Query("SELECT m FROM Message m WHERE m.senderDeleted = true AND m.receiverDeleted = true " +
           "AND ((m.senderDeletedAt < :threeYearsAgo) OR (m.receiverDeletedAt < :threeYearsAgo))")
    List<Message> findMessagesEligibleForPermanentDeletion(@Param("threeYearsAgo") java.time.LocalDateTime threeYearsAgo);
    
    // 편의 메서드
    default List<Message> findMessagesEligibleForPermanentDeletion() {
        return findMessagesEligibleForPermanentDeletion(java.time.LocalDateTime.now().minusYears(3));
    }
}