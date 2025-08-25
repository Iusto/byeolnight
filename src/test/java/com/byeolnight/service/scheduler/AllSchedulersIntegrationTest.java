package com.byeolnight.service.scheduler;

import com.byeolnight.service.PostCleanupScheduler;
import com.byeolnight.service.scheduler.NewsScheduler;
import com.byeolnight.service.scheduler.YoutubeScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@ActiveProfiles("test")
class AllSchedulersIntegrationTest {

    @Autowired private PostCleanupScheduler postCleanupScheduler;
    @MockBean private NewsScheduler newsScheduler;
    @MockBean private YoutubeScheduler youtubeScheduler;

    @Test
    @DisplayName("모든 스케줄러 통합 성능 테스트")
    void testAllSchedulersIntegrationPerformance() {
        doNothing().when(newsScheduler).collectNews();
        doNothing().when(youtubeScheduler).collectVideos();
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // 모든 스케줄러 순차 실행
        postCleanupScheduler.cleanupExpiredPosts();
        newsScheduler.collectNews();
        youtubeScheduler.collectVideos();
        
        stopWatch.stop();
        
        // 전체 실행 시간이 10초 이내인지 확인
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(10000);
    }

    @Test
    @DisplayName("스케줄러 병렬 실행 성능 테스트")
    void testSchedulersParallelPerformance() throws Exception {
        doNothing().when(newsScheduler).collectNews();
        doNothing().when(youtubeScheduler).collectVideos();
        
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // 병렬 실행
        CompletableFuture<Void> cleanup = CompletableFuture.runAsync(
            () -> postCleanupScheduler.cleanupExpiredPosts(), executor);
        CompletableFuture<Void> news = CompletableFuture.runAsync(
            () -> newsScheduler.collectNews(), executor);
        CompletableFuture<Void> youtube = CompletableFuture.runAsync(
            () -> youtubeScheduler.collectVideos(), executor);
        
        CompletableFuture.allOf(cleanup, news, youtube).get();
        
        stopWatch.stop();
        executor.shutdown();
        
        // 병렬 실행 시간이 5초 이내인지 확인
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(5000);
    }

    @Test
    @DisplayName("스케줄러 메모리 효율성 테스트")
    void testSchedulersMemoryEfficiency() {
        doNothing().when(newsScheduler).collectNews();
        doNothing().when(youtubeScheduler).collectVideos();
        
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // 모든 스케줄러 실행
        postCleanupScheduler.cleanupExpiredPosts();
        newsScheduler.collectNews();
        youtubeScheduler.collectVideos();
        
        System.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        
        // 메모리 증가량이 50MB 이내인지 확인
        long memoryIncrease = memoryAfter - memoryBefore;
        assertThat(memoryIncrease).isLessThan(50 * 1024 * 1024);
    }
}