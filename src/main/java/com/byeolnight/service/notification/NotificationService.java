package com.byeolnight.service.notification;

import com.byeolnight.domain.entity.Notification;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.NotificationRepository;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.dto.notification.NotificationDto;
import com.byeolnight.infrastructure.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // 알림 생성 및 실시간 전송
    @Transactional
    public void createNotification(Long userId, Notification.NotificationType type, 
                                 String title, String message, String targetUrl, Long relatedId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .targetUrl(targetUrl)
                .relatedId(relatedId)
                .build();

        Notification saved = notificationRepository.save(notification);
        
        // 실시간 알림 전송
        sendRealTimeNotification(user.getId(), NotificationDto.Response.from(saved));
    }

    // 사용자 알림 목록 조회
    public NotificationDto.ListResponse getNotifications(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        Page<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        return NotificationDto.ListResponse.builder()
                .notifications(notifications.getContent().stream()
                        .map(NotificationDto.Response::from)
                        .toList())
                .totalCount(notifications.getTotalElements())
                .currentPage(notifications.getNumber())
                .totalPages(notifications.getTotalPages())
                .hasNext(notifications.hasNext())
                .hasPrevious(notifications.hasPrevious())
                .build();
    }

    // 읽지 않은 알림 조회
    public List<NotificationDto.Response> getUnreadNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        System.out.println("=== 읽지 않은 알림 조회 시작 ===");
        System.out.println("요청 userId: " + userId);
        System.out.println("조회된 사용자: " + user.getNickname() + " (ID: " + user.getId() + ")");
        
        // 전체 알림 수 확인
        long totalNotifications = notificationRepository.count();
        System.out.println("전체 알림 수: " + totalNotifications);
        
        // 해당 사용자의 전체 알림 수 확인
        long userTotalNotifications = notificationRepository.countByUser(user);
        System.out.println("사용자 전체 알림 수: " + userTotalNotifications);
        
        List<Notification> notifications = notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
        System.out.println("조회된 읽지 않은 알림 수: " + notifications.size());
        
        if (!notifications.isEmpty()) {
            notifications.forEach(n -> {
                System.out.println("알림 상세: ID=" + n.getId() + ", 타입=" + n.getType() + ", 제목=" + n.getTitle() + ", 읽음여부=" + n.getIsRead() + ", 생성시간=" + n.getCreatedAt());
            });
        } else {
            System.out.println("읽지 않은 알림이 없습니다. 데이터베이스를 직접 확인해보세요.");
        }
        
        List<NotificationDto.Response> result = notifications.stream()
                .map(NotificationDto.Response::from)
                .toList();
                
        System.out.println("반환할 알림 DTO 수: " + result.size());
        System.out.println("=== 읽지 않은 알림 조회 완료 ===");
        return result;
    }

    // 읽지 않은 알림 개수
    public long getUnreadCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        long count = notificationRepository.countByUserAndIsReadFalse(user);
        System.out.println("=== 읽지 않은 알림 개수 조회 ===");
        System.out.println("userId: " + userId + ", 사용자: " + user.getNickname());
        System.out.println("읽지 않은 알림 개수: " + count);
        System.out.println("=== 개수 조회 완료 ===");
        return count;
    }

    // 알림 읽음 처리
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        notificationRepository.markAsReadByIdAndUser(notificationId, user);
    }

    // 모든 알림 읽음 처리
    @Transactional
    public void markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        notificationRepository.markAllAsReadByUser(user);
    }

    // 실시간 알림 전송
    private void sendRealTimeNotification(Long userId, NotificationDto.Response notification) {
        try {
            // 사용자별 개인 알림 전송
            messagingTemplate.convertAndSendToUser(
                userId.toString(), 
                "/queue/notifications", 
                notification
            );
            
            // 전체 브로드캐스트도 함께 전송 (디버깅용)
            messagingTemplate.convertAndSend(
                "/topic/notifications/" + userId,
                notification
            );
            
            log.info("실시간 알림 전송 완료: userId={}, type={}, title={}", userId, notification.getType(), notification.getTitle());
        } catch (Exception e) {
            log.error("실시간 알림 전송 실패: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    // 게시글 댓글 알림
    @Transactional
    public void notifyCommentOnPost(Long postAuthorId, Long postId, String commenterNickname) {
        if (postAuthorId == null) return;
        
        createNotification(
            postAuthorId,
            Notification.NotificationType.COMMENT_ON_POST,
            "새 댓글 알림",
            commenterNickname + "님이 회원님의 게시글에 댓글을 남겼습니다.",
            "/posts/" + postId,
            postId
        );
    }

    // 댓글 답글 알림
    @Transactional
    public void notifyReplyOnComment(Long commentAuthorId, Long postId, String replierNickname) {
        if (commentAuthorId == null) return;
        
        createNotification(
            commentAuthorId,
            Notification.NotificationType.REPLY_ON_COMMENT,
            "새 답글 알림",
            replierNickname + "님이 회원님의 댓글에 답글을 남겼습니다.",
            "/posts/" + postId,
            postId
        );
    }

    // 새 쪽지 알림
    @Transactional
    public void notifyNewMessage(Long receiverId, String senderNickname) {
        if (receiverId == null) {
            log.warn("쪽지 알림 전송 실패: receiverId가 null입니다.");
            return;
        }
        
        log.info("쪽지 알림 생성 시작: receiverId={}, senderNickname={}", receiverId, senderNickname);
        
        try {
            createNotification(
                receiverId,
                Notification.NotificationType.NEW_MESSAGE,
                "새 쪽지 알림",
                senderNickname + "님이 쪽지를 보냈습니다.",
                "/messages",
                null
            );
            log.info("쪽지 알림 생성 완료: receiverId={}", receiverId);
        } catch (Exception e) {
            log.error("쪽지 알림 생성 실패: receiverId={}, error={}", receiverId, e.getMessage(), e);
        }
    }

    // 새 공지사항 알림 (모든 사용자에게)
    @Transactional
    public void notifyNewNotice(Long noticeId, String noticeTitle) {
        List<User> allUsers = userRepository.findAll();
        
        for (User user : allUsers) {
            createNotification(
                user.getId(),
                Notification.NotificationType.NEW_NOTICE,
                "새 공지사항",
                "새로운 공지사항이 등록되었습니다: " + noticeTitle,
                "/posts/" + noticeId,
                noticeId
            );
        }
    }
    
    // 모든 사용자에게 알림 생성
    @Transactional
    public void createNotificationForAllUsers(Notification.NotificationType type, 
                                            String title, String message, String targetUrl, Long relatedId) {
        List<User> allUsers = userRepository.findAll();
        
        for (User user : allUsers) {
            createNotification(user.getId(), type, title, message, targetUrl, relatedId);
        }
    }
    
    // 알림 삭제
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("알림을 찾을 수 없습니다."));

        if (!notification.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("자신의 알림만 삭제할 수 있습니다.");
        }

        notificationRepository.delete(notification);
    }
}