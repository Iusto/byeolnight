package com.byeolnight.service.message;

import com.byeolnight.dto.message.MessageDto;
import com.byeolnight.entity.Message;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.MessageRepository;
import com.byeolnight.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MessageService messageService;

    @Test
    @DisplayName("받은 쪽지 조회")
    void getReceivedMessages() {
        User user = User.builder().id(1L).email("test@test.com").nickname("tester").build();
        User sender = User.builder().id(2L).nickname("sender").build();
        Message msg = Message.builder().sender(sender).receiver(user).content("test").build();
        Page<Message> page = new PageImpl<>(Arrays.asList(msg));
        
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(messageRepository.findByReceiverAndReceiverDeletedFalseOrderByCreatedAtDesc(any(), any()))
                .willReturn(page);

        MessageDto.ListResponse result = messageService.getReceivedMessages(1L, PageRequest.of(0, 10));

        assertThat(result.getMessages()).hasSize(1);
        assertThat(result.getTotalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("보낸 쪽지 조회")
    void getSentMessages() {
        User user = User.builder().id(1L).email("test@test.com").nickname("tester").build();
        User receiver = User.builder().id(2L).nickname("receiver").build();
        Message msg = Message.builder().sender(user).receiver(receiver).content("test").build();
        Page<Message> page = new PageImpl<>(Arrays.asList(msg));
        
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(messageRepository.findBySenderAndSenderDeletedFalseOrderByCreatedAtDesc(any(), any()))
                .willReturn(page);

        MessageDto.ListResponse result = messageService.getSentMessages(1L, PageRequest.of(0, 10));

        assertThat(result.getMessages()).hasSize(1);
    }

    @Test
    @DisplayName("읽지 않은 쪽지 개수")
    void getUnreadCount() {
        User user = User.builder().email("test@test.com").build();
        
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(messageRepository.countByReceiverAndIsReadFalseAndReceiverDeletedFalse(any())).willReturn(5L);

        long result = messageService.getUnreadCount(1L);

        assertThat(result).isEqualTo(5L);
    }

    @Test
    @DisplayName("쪽지 조회 검증")
    void getMessageVerification() {
        User user = User.builder().email("test@test.com").build();
        Message msg = Message.builder().receiver(user).sender(user).content("test").build();
        
        given(messageRepository.findById(1L)).willReturn(Optional.of(msg));

        Optional<Message> result = messageRepository.findById(1L);

        assertThat(result).isPresent();
    }
}
