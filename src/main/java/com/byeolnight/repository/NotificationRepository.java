package com.byeolnight.repository;

import com.byeolnight.entity.Notification;
import com.byeolnight.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 사용자별 알림 조회 (최신순)
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // 읽지 않은 알림 조회
    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

    // 읽지 않은 알림 개수
    long countByUserAndIsReadFalse(User user);

    // 사용자의 전체 알림 개수
    long countByUser(User user);

    // 사용자의 모든 알림을 읽음 처리
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    void markAllAsReadByUser(@Param("user") User user);

    // 특정 알림을 읽음 처리
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.user = :user")
    void markAsReadByIdAndUser(@Param("id") Long id, @Param("user") User user);
}