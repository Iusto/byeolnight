package com.byeolnight.controller.chat;

import com.byeolnight.common.TestSecurityConfig;
import com.byeolnight.dto.admin.ChatBanStatusDto;
import com.byeolnight.dto.chat.ChatMessageDto;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.security.JwtAuthenticationFilter;
import com.byeolnight.infrastructure.security.SecurityConfig;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import com.byeolnight.service.chat.AdminChatService;
import com.byeolnight.service.chat.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = ChatController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
        }
)
@Import(TestSecurityConfig.class)
@DisplayName("ChatController 테스트")
class ChatControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean ChatService chatService;
    @MockBean AdminChatService adminChatService;

    private final User mockUser = User.builder()
            .email("test@test.com")
            .nickname("tester")
            .password("encodedPassword")
            .role(User.Role.USER)
            .status(User.UserStatus.ACTIVE)
            .build();

    @Nested
    @DisplayName("채팅 메시지 조회 GET /api/public/chat (비로그인 허용)")
    class GetMessages {

        @Test
        @DisplayName("비로그인 상태에서 200 반환")
        void unauthenticated_returns200() throws Exception {
            when(chatService.getRecentMessages(eq("public"), anyInt()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/public/chat")
                            .param("roomId", "public"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("limit 파라미터 적용 시 200 반환")
        void withLimitParam_returns200() throws Exception {
            when(chatService.getRecentMessages(eq("public"), eq(50)))
                    .thenReturn(List.of(mock(ChatMessageDto.class)));

            mockMvc.perform(get("/api/public/chat")
                            .param("roomId", "public")
                            .param("limit", "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("채팅 이력 조회 GET /api/public/chat/history (비로그인 허용)")
    class GetChatHistory {

        @Test
        @DisplayName("비로그인 상태에서 200 반환")
        void unauthenticated_returns200() throws Exception {
            when(chatService.getMessagesBefore(any(), any(), anyInt()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/public/chat/history")
                            .param("roomId", "public")
                            .param("beforeId", "msg_123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("채팅 금지 상태 조회 GET /api/member/chat/ban-status (로그인 필요)")
    class GetChatBanStatus {

        @Test
        @DisplayName("비로그인 시 401 반환")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/member/chat/ban-status"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("로그인 상태에서 금지 아닌 경우 200 반환")
        void authenticated_notBanned_returns200() throws Exception {
            ChatBanStatusDto banStatus = ChatBanStatusDto.builder().banned(false).build();
            when(adminChatService.getUserBanStatus("tester")).thenReturn(banStatus);

            mockMvc.perform(get("/api/member/chat/ban-status")
                            .with(user(mockUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.banned").value(false));
        }

        @Test
        @DisplayName("로그인 상태에서 채팅 금지인 경우 200과 금지 정보 반환")
        void authenticated_banned_returns200WithBanInfo() throws Exception {
            ChatBanStatusDto banStatus = ChatBanStatusDto.builder().banned(true).reason("욕설 사용").build();
            when(adminChatService.getUserBanStatus("tester")).thenReturn(banStatus);

            mockMvc.perform(get("/api/member/chat/ban-status")
                            .with(user(mockUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.banned").value(true))
                    .andExpect(jsonPath("$.data.reason").value("욕설 사용"));
        }
    }
}
