package com.byeolnight.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 기반 캐시 서비스
 * - 기존 RedisTemplate과 함께 사용
 * - 분산 컬렉션 및 고급 기능 제공
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RedissonCacheService {

    private final RedissonClient redissonClient;

    private static final String INCREMENT_AND_GET_SCRIPT = """
        local key = KEYS[1]
        local ttl = tonumber(ARGV[1])

        local value = redis.call('INCR', key)
        if value == 1 and ttl > 0 then
            redis.call('EXPIRE', key, ttl)
        end
        return value
        """;

    /**
     * 값 저장 (TTL 포함)
     */
    public <T> void set(String key, T value, Duration ttl) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value, ttl.toSeconds(), TimeUnit.SECONDS);
        log.debug("캐시 저장: key={}, ttl={}초", key, ttl.toSeconds());
    }

    /**
     * 값 조회
     */
    public <T> T get(String key) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        T value = bucket.get();
        log.debug("캐시 조회: key={}, found={}", key, value != null);
        return value;
    }

    /**
     * 값 삭제
     */
    public boolean delete(String key) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        boolean deleted = bucket.delete();
        log.debug("캐시 삭제: key={}, deleted={}", key, deleted);
        return deleted;
    }

    /**
     * 키 존재 여부 확인
     */
    public boolean exists(String key) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        return bucket.isExists();
    }

    /**
     * 분산 캐시 맵 조회 (TTL 지원)
     */
    public <K, V> RMapCache<K, V> getMapCache(String mapName) {
        return redissonClient.getMapCache(mapName);
    }

    /**
     * 분산 맵에 값 저장 (TTL 포함)
     */
    public <K, V> void putToMap(String mapName, K key, V value, Duration ttl) {
        RMapCache<K, V> map = redissonClient.getMapCache(mapName);
        map.put(key, value, ttl.toSeconds(), TimeUnit.SECONDS);
        log.debug("분산 맵 저장: map={}, key={}, ttl={}초", mapName, key, ttl.toSeconds());
    }

    /**
     * 분산 맵에서 값 조회
     */
    public <K, V> V getFromMap(String mapName, K key) {
        RMapCache<K, V> map = redissonClient.getMapCache(mapName);
        V value = map.get(key);
        log.debug("분산 맵 조회: map={}, key={}, found={}", mapName, key, value != null);
        return value;
    }

    /**
     * 분산 맵에서 값 삭제
     */
    public <K, V> V removeFromMap(String mapName, K key) {
        RMapCache<K, V> map = redissonClient.getMapCache(mapName);
        V removed = map.remove(key);
        log.debug("분산 맵 삭제: map={}, key={}, removed={}", mapName, key, removed != null);
        return removed;
    }

    /**
     * 분산 맵 전체 조회
     */
    public <K, V> Map<K, V> getAllFromMap(String mapName) {
        RMapCache<K, V> map = redissonClient.getMapCache(mapName);
        return map.readAllMap();
    }

    /**
     * 분산 맵 크기 조회
     */
    public int getMapSize(String mapName) {
        RMapCache<Object, Object> map = redissonClient.getMapCache(mapName);
        return map.size();
    }

    /**
     * 분산 맵 전체 삭제
     */
    public boolean deleteMap(String mapName) {
        RMapCache<Object, Object> map = redissonClient.getMapCache(mapName);
        boolean deleted = map.delete();
        log.debug("분산 맵 전체 삭제: map={}, deleted={}", mapName, deleted);
        return deleted;
    }

    /**
     * 원자적 증가 연산 (INCR)
     * - 키가 없으면 1로 초기화
     * - TTL 설정 가능
     * @return 증가 후 값
     */
    public long incrementAndGet(String key, Duration ttl) {
        long ttlSeconds = (ttl != null) ? ttl.toSeconds() : 0;

        RScript script = redissonClient.getScript();
        long value = script.eval(
                RScript.Mode.READ_WRITE,
                INCREMENT_AND_GET_SCRIPT,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(key),
                ttlSeconds
        );

        log.debug("원자적 증가: key={}, value={}, ttl={}초", key, value, ttlSeconds > 0 ? ttlSeconds : "없음");
        return value;
    }

    /**
     * 원자적 카운터 값 조회
     */
    public long getCounter(String key) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        return atomicLong.get();
    }

    /**
     * 원자적 카운터 삭제
     */
    public boolean deleteCounter(String key) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        boolean deleted = atomicLong.delete();
        log.debug("카운터 삭제: key={}, deleted={}", key, deleted);
        return deleted;
    }

    // ============= Redis 메시지 큐 (Blocking Queue) =============

    /**
     * 메시지 큐에 작업 추가
     */
    public <T> void enqueue(String queueName, T job) {
        RBlockingQueue<T> queue = redissonClient.getBlockingQueue(queueName);
        queue.offer(job);
        log.debug("큐에 작업 추가: queue={}", queueName);
    }

    /**
     * 메시지 큐에서 작업 가져오기 (블로킹)
     * @param timeout 대기 시간
     * @return 작업 (타임아웃 시 null)
     */
    public <T> T dequeue(String queueName, Duration timeout) {
        RBlockingQueue<T> queue = redissonClient.getBlockingQueue(queueName);
        try {
            T job = queue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
            log.debug("큐에서 작업 가져옴: queue={}, found={}", queueName, job != null);
            return job;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("큐 대기 중 인터럽트 발생: queue={}", queueName);
            return null;
        }
    }

    /**
     * 메시지 큐 크기 조회
     */
    public int getQueueSize(String queueName) {
        RBlockingQueue<Object> queue = redissonClient.getBlockingQueue(queueName);
        return queue.size();
    }

    /**
     * 메시지 큐 전체 삭제
     */
    public boolean deleteQueue(String queueName) {
        RBlockingQueue<Object> queue = redissonClient.getBlockingQueue(queueName);
        boolean deleted = queue.delete();
        log.debug("큐 삭제: queue={}, deleted={}", queueName, deleted);
        return deleted;
    }
}