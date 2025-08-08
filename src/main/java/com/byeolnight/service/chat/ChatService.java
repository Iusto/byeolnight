package com.byeolnight.service.chat;

import com.byeolnight.entity.chat.ChatMessage;
import com.byeolnight.repository.chat.ChatMessageRepository;
import com.byeolnight.dto.chat.ChatMessageDto;
import com.byeolnight.entity.chat.ChatParticipation;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final com.byeolnight.repository.chat.ChatParticipationRepository chatParticipationRepository;
    private final UserRepository userRepository;
    private final com.byeolnight.service.certificate.CertificateService certificateService;

    public void sendMessage(ChatMessageDto dto) {
        ChatMessage entity = ChatMessage.builder()
                .roomId(dto.getRoomId())
                .sender(dto.getSender())
                .message(dto.getMessage())
                .build();
        
        ChatMessage saved = chatMessageRepository.save(entity);
        dto.setId(saved.getId().toString());
        messagingTemplate.convertAndSend("/topic/public", dto);
    }


    public List<ChatMessageDto> getRecentMessages(String roomId) {
        return getRecentMessages(roomId, 100);
    }
    
    public List<ChatMessageDto> getRecentMessages(String roomId, int limit) {
        return chatMessageRepository.findTop100ByRoomIdOrderByTimestampAsc(roomId,
                org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(entity -> ChatMessageDto.builder()
                        .id(entity.getId().toString())
                        .roomId(entity.getRoomId())
                        .sender(entity.getSender())
                        .message(entity.getMessage()) // 원본 메시지 유지
                        .ipAddress(entity.getIpAddress())
                        .timestamp(entity.getTimestamp())
                        .isBlinded(entity.getIsBlinded()) // 블라인드 상태 포함
                        .build())
                .toList();
    }
    
    public List<ChatMessageDto> getBlindedMessages(int limit) {
        return chatMessageRepository.findByIsBlindedTrueOrderByBlindedAtDesc(
                org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(entity -> ChatMessageDto.builder()
                        .id(entity.getId().toString())
                        .roomId(entity.getRoomId())
                        .sender(entity.getSender())
                        .message(entity.getMessage())
                        .ipAddress(entity.getIpAddress())
                        .timestamp(entity.getTimestamp())
                        .isBlinded(entity.getIsBlinded())
                        .build())
                .toList();
    }
    
    public List<ChatMessageDto> getMessagesBefore(String roomId, String beforeId, int limit) {
        Long beforeIdLong = Long.parseLong(beforeId);
        return chatMessageRepository.findByRoomIdAndIdLessThanOrderByTimestampDesc(roomId, beforeIdLong,
                org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(entity -> ChatMessageDto.builder()
                        .id(entity.getId().toString())
                        .roomId(entity.getRoomId())
                        .sender(entity.getSender())
                        .message(entity.getMessage())
                        .ipAddress(entity.getIpAddress())
                        .timestamp(entity.getTimestamp())
                        .isBlinded(entity.getIsBlinded())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    public void handleChatMessage(ChatMessageDto dto, Principal principal, boolean isPrivate) {
        String sender = extractSender(principal, dto);
        ChatMessageDto updatedDto = ChatMessageDto.builder()
                .roomId(dto.getRoomId())
                .sender(sender)
                .message(dto.getMessage())
                .timestamp(dto.getTimestamp())
                .build();

        save(updatedDto);
        messagingTemplate.convertAndSend("/topic/public", updatedDto);
    }

    public void handleDirectMessage(ChatMessageDto dto, Principal principal, String to) {
        String sender = extractSender(principal, dto);
        ChatMessageDto updatedDto = ChatMessageDto.builder()
                .roomId(dto.getRoomId())
                .sender(sender)
                .message(dto.getMessage())
                .timestamp(dto.getTimestamp())
                .build();

        save(updatedDto);
        messagingTemplate.convertAndSend("/queue/user." + to, updatedDto);
    }

    private String extractSender(Principal principal, ChatMessageDto dto) {
        if (principal != null) {
            return principal.getName();
        } else {
            log.warn("⚠️ Principal is null. fallback to DTO sender: {}", dto.getSender());
            return dto.getSender() != null ? dto.getSender() : "익명";
        }
    }

    public void save(ChatMessageDto dto) {
        save(dto, "unknown");
    }
    
    public void save(ChatMessageDto dto, String ipAddress) {
        if (dto.getMessage() == null || dto.getMessage().trim().isEmpty()) {
            log.warn("❌ 저장 거부: message가 null 또는 빈 문자열입니다. dto: {}", dto);
            return;
        }

        ChatMessage entity = ChatMessage.builder()
                .roomId(dto.getRoomId())
                .sender(dto.getSender())
                .message(dto.getMessage())
                .ipAddress(ipAddress)
                .build();
        
        ChatMessage saved = chatMessageRepository.save(entity);
        // DTO에 ID와 블라인드 상태 설정
        dto.setId(saved.getId().toString());
        dto.setIsBlinded(saved.getIsBlinded());
        
        // 채팅 참여 추적
        trackChatParticipation(dto.getSender());
    }
    
    private void trackChatParticipation(String nickname) {
        if (nickname == null || nickname.equals("익명")) {
            return;
        }
        
        try {
            User user = userRepository.findByNickname(nickname).orElse(null);
            if (user == null) {
                return;
            }
            
            java.time.LocalDate today = java.time.LocalDate.now();
            ChatParticipation participation =
                chatParticipationRepository.findByUserAndParticipationDate(user, today)
                    .orElse(ChatParticipation.of(user, today));
            
            if (participation.getId() == null) {
                chatParticipationRepository.save(participation);
            } else {
                participation.incrementMessageCount();
                chatParticipationRepository.save(participation);
            }
            
            // 채팅 인증서 체크
            certificateService.checkAndIssueCertificates(user, 
                com.byeolnight.service.certificate.CertificateService.CertificateCheckType.CHAT_PARTICIPATE);
                
        } catch (Exception e) {
            log.error("채팅 참여 추적 실패: {}", e.getMessage());
        }
    }
}
