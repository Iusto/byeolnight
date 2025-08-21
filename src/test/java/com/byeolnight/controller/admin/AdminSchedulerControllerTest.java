package com.byeolnight.controller.admin;

import com.byeolnight.service.PostCleanupScheduler;
import com.byeolnight.service.crawler.SpaceNewsScheduler;
import com.byeolnight.service.discussion.DiscussionTopicScheduler;
import com.byeolnight.service.message.MessageCleanupService;
import com.byeolnight.service.user.WithdrawnUserCleanupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminSchedulerController.class)
class AdminSchedulerControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private SpaceNewsScheduler spaceNewsScheduler;
    @MockBean private DiscussionTopicScheduler discussionTopicScheduler;
    @MockBean private PostCleanupScheduler postCleanupScheduler;
    @MockBean private MessageCleanupService messageCleanupService;
    @MockBean private WithdrawnUserCleanupService withdrawnUserCleanupService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("수동 뉴스 수집 실행")
    void testManualNewsCollection() throws Exception {
        doNothing().when(spaceNewsScheduler).scheduleSpaceNewsCollection();

        mockMvc.perform(post("/api/admin/scheduler/news/manual").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(spaceNewsScheduler).scheduleSpaceNewsCollection();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("수동 토론 주제 생성")
    void testManualDiscussionGeneration() throws Exception {
        doNothing().when(discussionTopicScheduler).generateDailyDiscussionTopic();

        mockMvc.perform(post("/api/admin/scheduler/discussion/manual").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(discussionTopicScheduler).generateDailyDiscussionTopic();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("모든 수동 스케줄러 실행 테스트")
    void testAllManualSchedulers() throws Exception {
        doNothing().when(postCleanupScheduler).cleanupExpiredPosts();
        doNothing().when(messageCleanupService).cleanupOldMessages();
        doNothing().when(withdrawnUserCleanupService).cleanupWithdrawnUsers();

        mockMvc.perform(post("/api/admin/scheduler/post-cleanup/manual").with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/scheduler/message-cleanup/manual").with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/scheduler/user-cleanup/manual").with(csrf()))
                .andExpect(status().isOk());

        verify(postCleanupScheduler).cleanupExpiredPosts();
        verify(messageCleanupService).cleanupOldMessages();
        verify(withdrawnUserCleanupService).cleanupWithdrawnUsers();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("스케줄러 예외 처리 테스트")
    void testSchedulerExceptionHandling() throws Exception {
        doThrow(new RuntimeException("테스트 예외"))
                .when(spaceNewsScheduler).scheduleSpaceNewsCollection();

        mockMvc.perform(post("/api/admin/scheduler/news/manual").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }
}