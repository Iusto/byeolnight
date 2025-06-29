package com.byeolnight.service.notification;

import com.byeolnight.domain.entity.Message;
import com.byeolnight.domain.entity.Notification;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.NotificationRepository;
import com.byeolnight.dto.notification.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Transactional
    public void sendMessageNotification(User receiver, User sender, Message message) {
        Notification notification = Notification.builder()
                .user(receiver)
                .type(Notification.NotificationType.MESSAGE)
                .title("새 쪽지가 도착했습니다")
                .content(sender.getNickname() + "님이 쪽지를 보냈습니다: " + message.getTitle())
                .referenceId(message.getId())
                .build();
        
        notificationRepository.save(notification);
        
        // WebSocket으로 실시간 알림 전송
        messagingTemplate.convertAndSendToUser(
                receiver.getId().toString(),
                "/queue/notifications",
                convertToResponse(notification)
        );
    }
    
    @Transactional
    public void sendCommentNotification(User receiver, User commenter, Long postId, String postTitle) {
        Notification notification = Notification.builder()
                .user(receiver)
                .type(Notification.NotificationType.COMMENT)
                .title("새 댓글이 달렸습니다")
                .content(commenter.getNickname() + "님이 '" + postTitle + "' 게시글에 댓글을 달았습니다")
                .referenceId(postId)
                .build();
        
        notificationRepository.save(notification);
        
        messagingTemplate.convertAndSendToUser(
                receiver.getId().toString(),
                "/queue/notifications",
                convertToResponse(notification)
        );
    }
    
    @Transactional
    public void sendReplyNotification(User receiver, User replier, Long commentId, String postTitle) {
        Notification notification = Notification.builder()
                .user(receiver)
                .type(Notification.NotificationType.REPLY)
                .title("새 답글이 달렸습니다")
                .content(replier.getNickname() + "님이 '" + postTitle + "' 게시글의 댓글에 답글을 달았습니다")
                .referenceId(commentId)
                .build();
        
        notificationRepository.save(notification);
        
        messagingTemplate.convertAndSendToUser(
                receiver.getId().toString(),
                "/queue/notifications",
                convertToResponse(notification)
        );
    }
    
    public Page<NotificationDto.Response> getNotifications(Long userId, Pageable pageable) {
        User user = User.builder().id(userId).build();
        
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::convertToResponse);
    }
    
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다"));
        
        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("접근 권한이 없습니다");
        }
        
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }
    
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다"));
        
        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("삭제 권한이 없습니다");
        }
        
        notificationRepository.delete(notification);
    }
    
    @Transactional
    public void clearAllNotifications(Long userId) {
        User user = User.builder().id(userId).build();
        notificationRepository.deleteByUser(user);
    }
    
    public long getUnreadCount(Long userId) {
        User user = User.builder().id(userId).build();
        return notificationRepository.countByUserAndIsReadFalse(user);
    }
    
    private NotificationDto.Response convertToResponse(Notification notification) {
        return NotificationDto.Response.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .referenceId(notification.getReferenceId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}