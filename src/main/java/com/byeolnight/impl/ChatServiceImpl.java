package com.byeolnight.impl;

import com.byeolnight.dto.chat.ChatMessageDto;
import com.byeolnight.domain.entity.chat.ChatMessageEntity;
import com.byeolnight.domain.repository.ChatMessageRepository;
import com.byeolnight.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ChatServiceImpl implements ChatService {
    private final ChatMessageRepository chatMessageRepository;

    @Override
    public void save(ChatMessageDto chatMessageDto) {
        chatMessageRepository.save(new ChatMessageEntity(chatMessageDto));
    }

    @Override
    public List<ChatMessageDto> getMessages(String roomId) {
        return chatMessageRepository.findByRoomId(roomId).stream()
                .map(entity -> new ChatMessageDto(
                        entity.getRoomId(),
                        entity.getSender(),
                        entity.getMessage(),
                        entity.getTimestamp()
                ))
                .collect(Collectors.toList());
    }
}