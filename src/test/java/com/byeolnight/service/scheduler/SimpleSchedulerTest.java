package com.byeolnight.service.scheduler;

import com.byeolnight.service.PostCleanupScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SimpleSchedulerTest {

    @Autowired private PostCleanupScheduler postCleanupScheduler;

    @Test
    @DisplayName("기본 스케줄러 실행 성능 테스트")
    void testBasicSchedulerPerformance() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        postCleanupScheduler.cleanupExpiredPosts();
        
        stopWatch.stop();
        
        // 기본 실행 시간이 3초 이내인지 확인
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(3000);
    }

    @Test
    @DisplayName("스케줄러 반복 실행 성능 테스트")
    void testRepeatedSchedulerPerformance() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // 5회 반복 실행
        for (int i = 0; i < 5; i++) {
            postCleanupScheduler.cleanupExpiredPosts();
        }
        
        stopWatch.stop();
        
        // 5회 실행 시간이 10초 이내인지 확인
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(10000);
    }

    @Test
    @DisplayName("스케줄러 CPU 사용률 테스트")
    void testSchedulerCpuUsage() {
        long startTime = System.nanoTime();
        
        postCleanupScheduler.cleanupExpiredPosts();
        
        long endTime = System.nanoTime();
        long executionTime = (endTime - startTime) / 1_000_000; // ms 변환
        
        // CPU 집약적 작업이 아님을 확인 (1초 이내)
        assertThat(executionTime).isLessThan(1000);
    }
}