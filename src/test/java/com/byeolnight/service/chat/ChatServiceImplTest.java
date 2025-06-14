package com.byeolnight.service.chat;

import com.byeolnight.domain.entity.chat.ChatMessageEntity;
import com.byeolnight.domain.repository.ChatMessageRepository;
import com.byeolnight.dto.chat.ChatMessageDto;
import com.byeolnight.impl.ChatServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Test
    void save_ShouldConvertDtoToEntityAndSave() {
        // given
        ChatMessageDto dto = new ChatMessageDto("room1", "user1", "hello", LocalDateTime.now());

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
        ChatMessageEntity e1 = new ChatMessageEntity(new ChatMessageDto("room1", "user1", "hi", LocalDateTime.now()));
        ChatMessageEntity e2 = new ChatMessageEntity(new ChatMessageDto("room1", "user2", "yo", LocalDateTime.now()));
        when(chatMessageRepository.findByRoomId("room1")).thenReturn(List.of(e1, e2));

        // when
        List<ChatMessageDto> result = chatService.getMessages("room1");

        // then
        assertEquals(2, result.size());
        assertEquals("user1", result.get(0).getSender());
        assertEquals("yo", result.get(1).getMessage());
    }
}
