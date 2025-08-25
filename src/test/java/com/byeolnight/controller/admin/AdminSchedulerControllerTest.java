package com.byeolnight.controller.admin;

import com.byeolnight.service.scheduler.NewsScheduler;
import com.byeolnight.service.scheduler.YoutubeScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureTestMvc
@ActiveProfiles("test")
class AdminSchedulerControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    
    @MockBean private NewsScheduler newsScheduler;
    @MockBean private YoutubeScheduler youtubeScheduler;

    @Test
    @DisplayName("뉴스 수집 API 성능 테스트")
    @WithMockUser(roles = "ADMIN")
    void testNewsCollectionPerformance() throws Exception {
        doNothing().when(newsScheduler).collectNews();
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        mockMvc.perform(post("/api/admin/scheduler/news/collect"))
                .andExpect(status().isOk());
        
        stopWatch.stop();
        
        // API 응답 시간이 2초 이내인지 확인
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(2000);
    }

    @Test
    @DisplayName("YouTube 수집 API 성능 테스트")
    @WithMockUser(roles = "ADMIN")
    void testYoutubeCollectionPerformance() throws Exception {
        doNothing().when(youtubeScheduler).collectVideos();
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        mockMvc.perform(post("/api/admin/scheduler/youtube/collect"))
                .andExpect(status().isOk());
        
        stopWatch.stop();
        
        // API 응답 시간이 2초 이내인지 확인
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(2000);
    }

    @Test
    @DisplayName("동시 스케줄러 실행 성능 테스트")
    @WithMockUser(roles = "ADMIN")
    void testConcurrentSchedulerPerformance() throws Exception {
        doNothing().when(newsScheduler).collectNews();
        doNothing().when(youtubeScheduler).collectVideos();
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // 동시 실행 시뮬레이션
        mockMvc.perform(post("/api/admin/scheduler/news/collect"));
        mockMvc.perform(post("/api/admin/scheduler/youtube/collect"));
        
        stopWatch.stop();
        
        // 동시 실행 시간이 3초 이내인지 확인
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(3000);
    }
}