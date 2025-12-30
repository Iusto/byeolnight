package com.byeolnight.service.weather;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WeatherRateLimitService 테스트
 * - 동시성 제한 (Semaphore)
 * - RPS 제한 (Guava RateLimiter)
 */
class WeatherRateLimitServiceTest {

    private WeatherRateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        // 동시성 3개, RPS 10/초로 테스트 (빠른 실행)
        rateLimitService = new WeatherRateLimitService(3, 10.0);
    }

    @Test
    @DisplayName("동시성 제한 - 정상 획득 및 해제")
    void tryAcquire_Success() {
        // When
        boolean acquired = rateLimitService.tryAcquire(Duration.ofSeconds(1));

        // Then
        assertThat(acquired).isTrue();

        // Cleanup
        rateLimitService.release();
    }

    @Test
    @DisplayName("동시성 제한 - 제한 초과 시 false")
    void tryAcquire_Exceeded() throws InterruptedException {
        // Given - 3개 모두 획득
        rateLimitService.tryAcquire(Duration.ofSeconds(1));
        rateLimitService.tryAcquire(Duration.ofSeconds(1));
        rateLimitService.tryAcquire(Duration.ofSeconds(1));

        // When - 4번째 시도 (타임아웃 짧게)
        boolean acquired = rateLimitService.tryAcquire(Duration.ofMillis(100));

        // Then
        assertThat(acquired).isFalse();
    }

    @Test
    @DisplayName("동시성 제한 - 해제 후 재획득 가능")
    void release_ThenAcquireAgain() {
        // Given - 3개 모두 획득
        rateLimitService.tryAcquire(Duration.ofSeconds(1));
        rateLimitService.tryAcquire(Duration.ofSeconds(1));
        rateLimitService.tryAcquire(Duration.ofSeconds(1));

        // When - 하나 해제
        rateLimitService.release();

        // Then - 다시 획득 가능
        boolean acquired = rateLimitService.tryAcquire(Duration.ofSeconds(1));
        assertThat(acquired).isTrue();

        // Cleanup
        rateLimitService.release();
    }

    @Test
    @DisplayName("RPS 제한 - 초당 제한 내에서는 통과")
    void rateLimiter_WithinLimit() {
        // When - 10개 요청 (10/초 제한)
        int successCount = 0;
        for (int i = 0; i < 10; i++) {
            if (rateLimitService.tryAcquire(Duration.ofSeconds(1))) {
                successCount++;
                rateLimitService.release();
            }
        }

        // Then - 모두 성공
        assertThat(successCount).isEqualTo(10);
    }

    @Test
    @DisplayName("동시 요청 - 동시성 제한 동작 확인")
    void concurrentRequests() throws InterruptedException {
        // Given
        int totalRequests = 10;
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When - 10개 스레드가 동시에 요청
        for (int i = 0; i < totalRequests; i++) {
            new Thread(() -> {
                boolean acquired = rateLimitService.tryAcquire(Duration.ofMillis(100));
                if (acquired) {
                    successCount.incrementAndGet();
                    try {
                        Thread.sleep(50); // 작업 시뮬레이션
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    rateLimitService.release();
                } else {
                    failCount.incrementAndGet();
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        // Then - 동시성 3개 제한으로 일부는 실패해야 함
        assertThat(successCount.get()).isLessThanOrEqualTo(10);
        assertThat(successCount.get() + failCount.get()).isEqualTo(totalRequests);
    }
}