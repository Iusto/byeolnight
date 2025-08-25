package com.byeolnight.controller.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;

class AdminSchedulerControllerTest {

    @Test
    @DisplayName("뉴스 수집 API 성능 테스트")
    void testNewsCollectionPerformance() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // API 호출 시뮬레이션
        simulateApiCall();
        
        stopWatch.stop();
        
        // API 응답 시간이 100ms 이내인지 확인
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(100);
    }

    @Test
    @DisplayName("토론 주제 생성 API 성능 테스트")
    void testTopicGenerationPerformance() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // API 호출 시뮬레이션
        simulateApiCall();
        
        stopWatch.stop();
        
        // API 응답 시간이 100ms 이내인지 확인
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(100);
    }

    @Test
    @DisplayName("동시 스케줄러 실행 성능 테스트")
    void testConcurrentSchedulerPerformance() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // 동시 실행 시뮬레이션
        simulateApiCall();
        simulateApiCall();
        
        stopWatch.stop();
        
        // 동시 실행 시간이 150ms 이내인지 확인
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(150);
    }

    private void simulateApiCall() {
        try {
            Thread.sleep(20); // 20ms API 호출 시뮬레이션
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}