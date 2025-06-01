package com.byeolnight.service.chat;

import com.byeolnight.dto.chat.ChatMessageDto;

import java.util.List;

public interface ChatService {
    void save(ChatMessageDto chatMessageDto);
    List<ChatMessageDto> getMessages(String roomId);
}