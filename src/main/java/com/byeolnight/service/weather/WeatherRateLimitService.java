package com.byeolnight.service.weather;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 날씨 API 호출 제한 서비스
 * - Semaphore: 동시성 제한
 * - Guava RateLimiter: RPS 제한
 */
@Slf4j
@Service
public class WeatherRateLimitService {

    private final Semaphore semaphore;
    private final RateLimiter rateLimiter;

    /**
     * 기본 생성자
     * - 동시성: 10개
     * - RPS: 5/초
     */
    public WeatherRateLimitService() {
        this(10, 5.0);
    }

    /**
     * 테스트용 생성자
     */
    public WeatherRateLimitService(int maxConcurrent, double permitsPerSecond) {
        this.semaphore = new Semaphore(maxConcurrent);
        this.rateLimiter = RateLimiter.create(permitsPerSecond);
        log.info("WeatherRateLimitService initialized: maxConcurrent={}, permitsPerSecond={}",
                 maxConcurrent, permitsPerSecond);
    }

    /**
     * 호출 제한 획득 시도
     * - Semaphore와 RateLimiter 모두 통과해야 함
     *
     * @param timeout 대기 시간
     * @return 획득 성공 여부
     */
    public boolean tryAcquire(Duration timeout) {
        try {
            // 1. Semaphore 획득 시도 (동시성 제한)
            boolean semaphoreAcquired = semaphore.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!semaphoreAcquired) {
                log.warn("Semaphore 획득 실패 (동시성 제한 초과)");
                return false;
            }

            // 2. RateLimiter 획득 시도 (RPS 제한)
            boolean rateLimitAcquired = rateLimiter.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!rateLimitAcquired) {
                log.warn("RateLimiter 획득 실패 (RPS 제한 초과)");
                semaphore.release(); // Semaphore 반환
                return false;
            }

            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Rate limit 획득 중 인터럽트 발생", e);
            return false;
        }
    }

    /**
     * Semaphore 해제
     * - tryAcquire()로 획득한 경우에만 호출해야 함
     */
    public void release() {
        semaphore.release();
    }
}
