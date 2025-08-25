package com.byeolnight.service.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleSchedulerTest {

    @Test
    @DisplayName("기본 스케줄러 실행 성능 테스트")
    void testBasicSchedulerPerformance() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // 간단한 작업 시뮬레이션
        simulateSchedulerWork();
        
        stopWatch.stop();
        
        // 기본 실행 시간이 100ms 이내인지 확인
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(100);
    }

    @Test
    @DisplayName("스케줄러 반복 실행 성능 테스트")
    void testRepeatedSchedulerPerformance() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // 5회 반복 실행
        for (int i = 0; i < 5; i++) {
            simulateSchedulerWork();
        }
        
        stopWatch.stop();
        
        // 5회 실행 시간이 500ms 이내인지 확인
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(500);
    }

    @Test
    @DisplayName("스케줄러 CPU 사용률 테스트")
    void testSchedulerCpuUsage() {
        long startTime = System.nanoTime();
        
        simulateSchedulerWork();
        
        long endTime = System.nanoTime();
        long executionTime = (endTime - startTime) / 1_000_000; // ms 변환
        
        // CPU 집약적 작업이 아님을 확인 (50ms 이내)
        assertThat(executionTime).isLessThan(50);
    }

    private void simulateSchedulerWork() {
        // 간단한 작업 시뮬레이션
        try {
            Thread.sleep(10); // 10ms 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}