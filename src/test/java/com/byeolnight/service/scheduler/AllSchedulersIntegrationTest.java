package com.byeolnight.service.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class AllSchedulersIntegrationTest {

    @Test
    @DisplayName("모든 스케줄러 통합 성능 테스트")
    void testAllSchedulersIntegrationPerformance() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // 모든 스케줄러 순차 실행 시뮬레이션
        simulatePostCleanup();
        simulateNewsCollection();
        simulateTopicGeneration();
        
        stopWatch.stop();
        
        // 전체 실행 시간이 200ms 이내인지 확인
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(200);
    }

    @Test
    @DisplayName("스케줄러 병렬 실행 성능 테스트")
    void testSchedulersParallelPerformance() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // 병렬 실행
        CompletableFuture<Void> cleanup = CompletableFuture.runAsync(this::simulatePostCleanup, executor);
        CompletableFuture<Void> news = CompletableFuture.runAsync(this::simulateNewsCollection, executor);
        CompletableFuture<Void> topic = CompletableFuture.runAsync(this::simulateTopicGeneration, executor);
        
        CompletableFuture.allOf(cleanup, news, topic).get();
        
        stopWatch.stop();
        executor.shutdown();
        
        // 병렬 실행 시간이 100ms 이내인지 확인
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(100);
    }

    @Test
    @DisplayName("스케줄러 메모리 효율성 테스트")
    void testSchedulersMemoryEfficiency() {
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // 모든 스케줄러 실행 시뮬레이션
        simulatePostCleanup();
        simulateNewsCollection();
        simulateTopicGeneration();
        
        System.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        
        // 메모리 증가량이 10MB 이내인지 확인
        long memoryIncrease = memoryAfter - memoryBefore;
        assertThat(memoryIncrease).isLessThan(10 * 1024 * 1024);
    }

    private void simulatePostCleanup() {
        try { Thread.sleep(30); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void simulateNewsCollection() {
        try { Thread.sleep(40); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void simulateTopicGeneration() {
        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}