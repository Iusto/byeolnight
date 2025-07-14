package com.byeolnight.service.message;

import com.byeolnight.domain.entity.Message;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.MessageRepository;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.dto.message.MessageRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("쪽지 서비스 테스트")
class MessageServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private MessageService messageService;

    private User sender;
    private User receiver;
    private Message message;

    @BeforeEach
    void setUp() {
        sender = User.builder()
                .email("sender@example.com")
                .nickname("발신자")
                .password("password")
                .phone("01012345678")
                .role(User.Role.USER)
                .build();
        ReflectionTestUtils.setField(sender, "id", 1L);

        receiver = User.builder()
                .email("receiver@example.com")
                .nickname("수신자")
                .password("password")
                .phone("01012345679")
                .role(User.Role.USER)
                .build();
        ReflectionTestUtils.setField(receiver, "id", 2L);

        message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .title("테스트 쪽지")
                .content("테스트 내용")
                .build();
        ReflectionTestUtils.setField(message, "id", 1L);
    }

    @Test
    @DisplayName("쪽지 전송 성공")
    void sendMessage_success() {
        MessageRequestDto dto = new MessageRequestDto("테스트 제목", "테스트 내용", 2L);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        Long messageId = messageService.sendMessage(1L, dto);

        assertThat(messageId).isEqualTo(1L);
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    @DisplayName("받은 쪽지 목록 조회")
    void getReceivedMessages_success() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Message> page = new PageImpl<>(List.of(message));
        
        when(messageRepository.findReceivedMessages(2L, pageRequest)).thenReturn(page);

        Page<Message> result = messageService.getReceivedMessages(2L, pageRequest);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("테스트 쪽지");
    }

    @Test
    @DisplayName("보낸 쪽지 목록 조회")
    void getSentMessages_success() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Message> page = new PageImpl<>(List.of(message));
        
        when(messageRepository.findSentMessages(1L, pageRequest)).thenReturn(page);

        Page<Message> result = messageService.getSentMessages(1L, pageRequest);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("테스트 쪽지");
    }

    @Test
    @DisplayName("읽지 않은 쪽지 개수 조회")
    void getUnreadCount_success() {
        when(messageRepository.countUnreadMessages(2L)).thenReturn(3L);

        long count = messageService.getUnreadCount(2L);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    @DisplayName("쪽지 읽음 처리")
    void markAsRead_success() {
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));

        messageService.markAsRead(1L, 2L);

        assertThat(message.isRead()).isTrue();
        verify(messageRepository).save(message);
    }

    @Test
    @DisplayName("쪽지 삭제 (수신자)")
    void deleteMessage_receiver() {
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));

        messageService.deleteMessage(1L, 2L);

        assertThat(message.getReceiverDeleted()).isTrue();
        verify(messageRepository).save(message);
    }

    @Test
    @DisplayName("쪽지 삭제 (발신자)")
    void deleteMessage_sender() {
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));

        messageService.deleteMessage(1L, 1L);

        assertThat(message.getSenderDeleted()).isTrue();
        verify(messageRepository).save(message);
    }
}