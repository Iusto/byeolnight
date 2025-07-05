package com.byeolnight.service.message;

import com.byeolnight.domain.entity.Message;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.MessageRepository;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.dto.message.MessageDto;
import com.byeolnight.infrastructure.exception.NotFoundException;
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

    // 쪽지 전송
    @Transactional
    public MessageDto.Response sendMessage(Long senderId, MessageDto.SendRequest request) {
        if (senderId == null) {
            throw new IllegalArgumentException("발신자 ID가 필요합니다.");
        }
        if (request.getReceiverId() == null) {
            throw new IllegalArgumentException("수신자 ID가 필요합니다.");
        }
        
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new NotFoundException("발신자를 찾을 수 없습니다."));
        
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new NotFoundException("수신자를 찾을 수 없습니다."));

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        Message saved = messageRepository.save(message);

        // 쪽지 알림 전송
        notificationService.notifyNewMessage(receiver.getId(), sender.getNickname());

        return MessageDto.Response.from(saved);
    }

    // 받은 쪽지함
    public MessageDto.ListResponse getReceivedMessages(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        Page<Message> messages = messageRepository.findByReceiverAndReceiverDeletedFalseOrderByCreatedAtDesc(user, pageable);

        return MessageDto.ListResponse.builder()
                .messages(messages.getContent().stream()
                        .map(MessageDto.Response::from)
                        .toList())
                .totalCount(messages.getTotalElements())
                .currentPage(messages.getNumber())
                .totalPages(messages.getTotalPages())
                .hasNext(messages.hasNext())
                .hasPrevious(messages.hasPrevious())
                .build();
    }

    // 보낸 쪽지함
    public MessageDto.ListResponse getSentMessages(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        Page<Message> messages = messageRepository.findBySenderAndSenderDeletedFalseOrderByCreatedAtDesc(user, pageable);

        return MessageDto.ListResponse.builder()
                .messages(messages.getContent().stream()
                        .map(MessageDto.Response::from)
                        .toList())
                .totalCount(messages.getTotalElements())
                .currentPage(messages.getNumber())
                .totalPages(messages.getTotalPages())
                .hasNext(messages.hasNext())
                .hasPrevious(messages.hasPrevious())
                .build();
    }

    // 쪽지 상세 조회 및 읽음 처리
    @Transactional
    public MessageDto.Response getMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("쪽지를 찾을 수 없습니다."));

        // 권한 확인 (발신자 또는 수신자만 조회 가능)
        if (!message.getSender().getId().equals(userId) && !message.getReceiver().getId().equals(userId)) {
            throw new NotFoundException("쪽지를 찾을 수 없습니다.");
        }

        // 수신자가 조회하는 경우 읽음 처리
        if (message.getReceiver().getId().equals(userId) && !message.getIsRead()) {
            message.markAsRead();
        }

        return MessageDto.Response.from(message);
    }

    // 읽지 않은 쪽지 개수
    public long getUnreadCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        return messageRepository.countByReceiverAndIsReadFalseAndReceiverDeletedFalse(user);
    }

    // 쪽지 삭제
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("쪽지를 찾을 수 없습니다."));

        // 발신자가 삭제하는 경우
        if (message.getSender().getId().equals(userId)) {
            message.deleteBySender();
        }
        // 수신자가 삭제하는 경우
        else if (message.getReceiver().getId().equals(userId)) {
            message.deleteByReceiver();
        }
        else {
            throw new NotFoundException("쪽지를 찾을 수 없습니다.");
        }

        // 양쪽 모두 삭제한 경우 실제 삭제
        if (message.getSenderDeleted() && message.getReceiverDeleted()) {
            messageRepository.delete(message);
        }
    }
}