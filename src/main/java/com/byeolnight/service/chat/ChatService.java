package com.byeolnight.service.chat;

import com.byeolnight.domain.entity.chat.ChatMessage;
import com.byeolnight.domain.repository.chat.ChatMessageRepository;
import com.byeolnight.dto.chat.ChatMessageDto;
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
        return chatMessageRepository.findTop100ByRoomIdOrderByTimestampAsc(roomId,
                org.springframework.data.domain.PageRequest.of(0, 100))
                .stream()
                .map(entity -> ChatMessageDto.builder()
                        .id(entity.getId().toString())
                        .roomId(entity.getRoomId())
                        .sender(entity.getSender())
                        .message(entity.getIsBlinded() ? "🙈 블라인드 처리된 메시지" : entity.getMessage())
                        .timestamp(entity.getTimestamp())
                        .build())
                .toList();
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
        if (dto.getMessage() == null || dto.getMessage().trim().isEmpty()) {
            log.warn("❌ 저장 거부: message가 null 또는 빈 문자열입니다. dto: {}", dto);
            return;
        }

        ChatMessage entity = ChatMessage.builder()
                .roomId(dto.getRoomId())
                .sender(dto.getSender())
                .message(dto.getMessage())
                .build();
        
        ChatMessage saved = chatMessageRepository.save(entity);
        // DTO에 ID 설정 (프론트엔드에서 사용)
        dto.setId(saved.getId().toString());
    }
}
