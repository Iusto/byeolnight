package com.byeolnight.infrastructure.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 분산락 서비스
 * - Redisson을 이용한 분산락 구현
 * - 동시성 제어가 필요한 비즈니스 로직에 사용
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DistributedLockService {

    private final RedissonClient redissonClient;

    /**
     * 분산락을 사용하여 작업 실행
     * 
     * @param lockKey 락 키
     * @param waitTime 락 획득 대기 시간 (초)
     * @param leaseTime 락 보유 시간 (초)
     * @param task 실행할 작업
     * @return 작업 결과
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, Supplier<T> task) {
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            
            if (!acquired) {
                log.warn("락 획득 실패: {}", lockKey);
                throw new IllegalStateException("락을 획득할 수 없습니다. 잠시 후 다시 시도해주세요.");
            }
            
            log.debug("락 획득 성공: {}", lockKey);
            return task.get();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("락 획득 중 인터럽트 발생: {}", lockKey, e);
            throw new RuntimeException("락 획득 중 오류가 발생했습니다.", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("락 해제 완료: {}", lockKey);
            }
        }
    }

    /**
     * 분산락을 사용하여 작업 실행 (반환값 없음)
     */
    public void executeWithLock(String lockKey, long waitTime, long leaseTime, Runnable task) {
        executeWithLock(lockKey, waitTime, leaseTime, () -> {
            task.run();
            return null;
        });
    }

    /**
     * 기본 설정으로 분산락 실행 (대기 5초, 보유 10초)
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> task) {
        return executeWithLock(lockKey, 5, 10, task);
    }

    /**
     * 기본 설정으로 분산락 실행 (반환값 없음)
     */
    public void executeWithLock(String lockKey, Runnable task) {
        executeWithLock(lockKey, 5, 10, task);
    }
}