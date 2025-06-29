package com.byeolnight.domain.repository;

import com.byeolnight.domain.entity.Notification;
import com.byeolnight.domain.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // 사용자의 알림 목록
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    // 읽지 않은 알림 개수
    long countByUserAndIsReadFalse(User user);
    
    // 사용자의 모든 알림 삭제
    void deleteByUser(User user);
}