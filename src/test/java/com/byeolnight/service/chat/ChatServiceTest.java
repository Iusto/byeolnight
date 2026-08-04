package com.byeolnight.service.chat;

import com.byeolnight.dto.chat.ChatMessageDto;
import com.byeolnight.infrastructure.exception.InvalidRequestException;
import com.byeolnight.repository.chat.ChatMessageRepository;
import com.byeolnight.repository.chat.ChatParticipationRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.service.certificate.CertificateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService 테스트")
class ChatServiceTest {

    @Mock ChatMessageRepository chatMessageRepository;
    @Mock ChatParticipationRepository chatParticipationRepository;
    @Mock UserRepository userRepository;
    @Mock CertificateService certificateService;

    @InjectMocks ChatService chatService;

    @Nested
    @DisplayName("무한 스크롤 커서")
    class Cursor {

        @Test
        @DisplayName("숫자가 아닌 커서는 400으로 처리한다")
        void rejectsNonNumericCursor() {
            assertThatThrownBy(() -> chatService.getMessagesBefore("public", "msg_123", 20))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("beforeId");
        }

        @Test
        @DisplayName("빈 커서도 400으로 처리한다")
        void rejectsEmptyCursor() {
            assertThatThrownBy(() -> chatService.getMessagesBefore("public", "", 20))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("정상 커서는 id 기준으로 조회한다")
        void queriesByIdCursor() {
            when(chatMessageRepository.findByRoomIdAndIdLessThanOrderByIdDesc(
                    eq("public"), eq(100L), any(Pageable.class)))
                    .thenReturn(List.of());

            List<ChatMessageDto> result = chatService.getMessagesBefore("public", "100", 20);

            assertThat(result).isEmpty();
            verify(chatMessageRepository).findByRoomIdAndIdLessThanOrderByIdDesc(
                    eq("public"), eq(100L), any(Pageable.class));
        }
    }
}
