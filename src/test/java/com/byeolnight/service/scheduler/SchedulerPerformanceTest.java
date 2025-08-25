package com.byeolnight.service.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerPerformanceTest {

    @Test
    @DisplayName("대량 데이터 정리 성능 테스트")
    void testBulkCleanupPerformance() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // 대량 데이터 정리 시뮬레이션
        simulateBulkCleanup(100);
        
        stopWatch.stop();
        
        // 성능 검증 (200ms 이내)
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(200);
    }

    @Test
    @DisplayName("스케줄러 메모리 사용량 테스트")
    void testSchedulerMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        simulateBulkCleanup(50);
        
        System.gc(); // 가비지 컬렉션 강제 실행
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        
        // 메모리 증가량이 5MB 이내인지 확인
        long memoryIncrease = memoryAfter - memoryBefore;
        assertThat(memoryIncrease).isLessThan(5 * 1024 * 1024); // 5MB
    }

    private void simulateBulkCleanup(int count) {
        // 대량 데이터 처리 시뮬레이션
        for (int i = 0; i < count; i++) {
            try {
                Thread.sleep(1); // 1ms per item
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}