package com.byeolnight.service.chat;

import com.byeolnight.domain.entity.chat.ChatMessageEntity;
import com.byeolnight.domain.repository.ChatMessageRepository;
import com.byeolnight.dto.chat.ChatMessageDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private ChatService chatService;

    @Test
    void save_ShouldConvertDtoToEntityAndSave() {
        // given
        ChatMessageDto dto = ChatMessageDto.builder()
                .roomId("room1")
                .sender("user1")
                .message("hello")
                .timestamp(LocalDateTime.now())
                .build();

        // when
        chatService.save(dto);

        // then
        ArgumentCaptor<ChatMessageEntity> captor = ArgumentCaptor.forClass(ChatMessageEntity.class);
        verify(chatMessageRepository, times(1)).save(captor.capture());

        ChatMessageEntity saved = captor.getValue();
        assertEquals(dto.getRoomId(), saved.getRoomId());
        assertEquals(dto.getSender(), saved.getSender());
        assertEquals(dto.getMessage(), saved.getMessage());
    }

    @Test
    void getMessages_ShouldReturnListOfDto() {
        // given
        ChatMessageEntity e1 = new ChatMessageEntity(ChatMessageDto.builder()
                .roomId("room1")
                .sender("user1")
                .message("hi")
                .timestamp(LocalDateTime.now())
                .build());

        ChatMessageEntity e2 = new ChatMessageEntity(ChatMessageDto.builder()
                .roomId("room1")
                .sender("user2")
                .message("yo")
                .timestamp(LocalDateTime.now())
                .build());

        when(chatMessageRepository.findTop100ByRoomIdOrderByTimestampAsc("room1"))
                .thenReturn(List.of(e1, e2));

        // when
        List<ChatMessageDto> result = chatService.getRecentMessages("room1");

        // then
        assertEquals(2, result.size());
        assertEquals("user1", result.get(0).getSender());
        assertEquals("yo", result.get(1).getMessage());
    }
}
