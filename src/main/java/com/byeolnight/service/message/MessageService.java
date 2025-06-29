package com.byeolnight.service.message;

import com.byeolnight.domain.entity.Message;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.MessageRepository;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.dto.message.MessageDto;
import com.byeolnight.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    
    @Transactional
    public MessageDto.Response sendMessage(Long senderId, MessageDto.Request request) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("발신자를 찾을 수 없습니다"));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new RuntimeException("수신자를 찾을 수 없습니다"));
        
        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .title(request.getTitle())
                .content(request.getContent())
                .build();
        
        Message savedMessage = messageRepository.save(message);
        
        // 실시간 알림 전송
        notificationService.sendMessageNotification(receiver, sender, savedMessage);
        
        return convertToResponse(savedMessage);
    }
    
    public Page<MessageDto.Summary> getReceivedMessages(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        return messageRepository.findByReceiverOrderByCreatedAtDesc(user, pageable)
                .map(this::convertToSummary);
    }
    
    public Page<MessageDto.Summary> getSentMessages(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        return messageRepository.findBySenderOrderByCreatedAtDesc(user, pageable)
                .map(this::convertToSummary);
    }
    
    public MessageDto.Response getMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("쪽지를 찾을 수 없습니다"));
        
        // 권한 확인
        if (!message.getSender().getId().equals(userId) && 
            !message.getReceiver().getId().equals(userId)) {
            throw new RuntimeException("접근 권한이 없습니다");
        }
        
        return convertToResponse(message);
    }
    
    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("쪽지를 찾을 수 없습니다"));
        
        if (message.getReceiver().getId().equals(userId) && !message.getIsRead()) {
            message.setIsRead(true);
            messageRepository.save(message);
        }
    }
    
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("쪽지를 찾을 수 없습니다"));
        
        if (!message.getSender().getId().equals(userId) && 
            !message.getReceiver().getId().equals(userId)) {
            throw new RuntimeException("삭제 권한이 없습니다");
        }
        
        messageRepository.delete(message);
    }
    
    public long getUnreadCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        return messageRepository.countByReceiverAndIsReadFalse(user);
    }
    
    private MessageDto.Response convertToResponse(Message message) {
        return MessageDto.Response.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .senderNickname(message.getSender().getNickname())
                .receiverId(message.getReceiver().getId())
                .receiverNickname(message.getReceiver().getNickname())
                .title(message.getTitle())
                .content(message.getContent())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .build();
    }
    
    private MessageDto.Summary convertToSummary(Message message) {
        return MessageDto.Summary.builder()
                .id(message.getId())
                .senderNickname(message.getSender().getNickname())
                .title(message.getTitle())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .build();
    }
}