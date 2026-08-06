package com.byeolnight.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

/**
 * Redis 캐시 서비스 (StringRedisTemplate 기반)
 * - 모든 값을 String으로 통일 (객체는 JSON 직렬화/역직렬화 명시적 처리)
 * - 원자적 증가: Lua 스크립트 (INCR + 조건부 EXPIRE)
 * - 비동기 큐: List 자료구조 (LPUSH / BRPOP)
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RedisCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>("""
            local key = KEYS[1]
            local ttl = tonumber(ARGV[1])
            local value = redis.call('INCR', key)
            if value == 1 and ttl > 0 then
                redis.call('EXPIRE', key, ttl)
            end
            return value
            """, Long.class);

    private static final DefaultRedisScript<String> DEQUEUE_DUE_SCRIPT = new DefaultRedisScript<>("""
            local jobs = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1], 'LIMIT', 0, 1)
            if #jobs == 0 then
                return nil
            end
            if redis.call('ZREM', KEYS[1], jobs[1]) == 1 then
                return jobs[1]
            end
            return nil
            """, String.class);

    // ============= String 값 저장/조회/삭제 =============

    public <T> void set(String key, T value, Duration ttl) {
        stringRedisTemplate.opsForValue().set(key, (String) value, ttl);
        log.debug("캐시 저장: key={}, ttl={}초", key, ttl.toSeconds());
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        T value = (T) stringRedisTemplate.opsForValue().get(key);
        log.debug("캐시 조회: key={}, found={}", key, value != null);
        return value;
    }

    public boolean delete(String key) {
        boolean deleted = Boolean.TRUE.equals(stringRedisTemplate.delete(key));
        log.debug("캐시 삭제: key={}, deleted={}", key, deleted);
        return deleted;
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    // ============= 원자적 카운터 (Lua INCR + EXPIRE) =============

    /**
     * 원자적 증가 연산
     * - 키가 없으면 1로 초기화하면서 TTL 동시 설정 (Lua 스크립트로 원자성 보장)
     */
    public long incrementAndGet(String key, Duration ttl) {
        long ttlSeconds = (ttl != null) ? ttl.toSeconds() : 0;
        Long value = stringRedisTemplate.execute(
                INCREMENT_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(ttlSeconds)
        );
        long result = value != null ? value : 0L;
        log.debug("원자적 증가: key={}, value={}", key, result);
        return result;
    }

    public long getCounter(String key) {
        String val = stringRedisTemplate.opsForValue().get(key);
        if (val == null) return 0L;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public boolean deleteCounter(String key) {
        boolean deleted = Boolean.TRUE.equals(stringRedisTemplate.delete(key));
        log.debug("카운터 삭제: key={}, deleted={}", key, deleted);
        return deleted;
    }

    // ============= 객체 큐 (LPUSH / BRPOP) =============

    /**
     * 큐에 작업 추가 - 객체를 JSON 문자열로 직렬화 후 저장
     */
    public <T> void enqueue(String queueName, T job) {
        try {
            String json = objectMapper.writeValueAsString(job);
            stringRedisTemplate.opsForList().leftPush(queueName, json);
            log.debug("큐에 작업 추가: queue={}", queueName);
        } catch (Exception e) {
            throw new RuntimeException("큐 직렬화 실패: " + queueName, e);
        }
    }

    /**
     * 큐에서 작업 가져오기 (BRPOP, 블로킹) - JSON 문자열을 객체로 역직렬화
     */
    public <T> T dequeue(String queueName, Duration timeout, Class<T> type) {
        try {
            String json = stringRedisTemplate.opsForList().rightPop(queueName, timeout);
            if (json == null) return null;
            T job = objectMapper.readValue(json, type);
            log.debug("큐에서 작업 가져옴: queue={}", queueName);
            return job;
        } catch (Exception e) {
            log.warn("큐 역직렬화 실패: queue={}, error={}", queueName, e.getMessage());
            return null;
        }
    }

    // ============= 지연 큐 (Sorted Set) =============

    /**
     * 지정된 시각 이후에만 소비할 수 있는 작업을 저장한다.
     */
    public <T> void enqueueDelayed(String queueName, T job, Instant dueAt) {
        try {
            String json = objectMapper.writeValueAsString(job);
            stringRedisTemplate.opsForZSet().add(queueName, json, dueAt.toEpochMilli());
            log.debug("지연 큐에 작업 추가: queue={}, dueAt={}", queueName, dueAt);
        } catch (Exception e) {
            throw new RuntimeException("지연 큐 직렬화 실패: " + queueName, e);
        }
    }

    /**
     * 실행 시각이 지난 작업 한 건을 원자적으로 가져온다.
     */
    public <T> T dequeueDue(String queueName, Instant now, Class<T> type) {
        try {
            String json = stringRedisTemplate.execute(
                    DEQUEUE_DUE_SCRIPT,
                    Collections.singletonList(queueName),
                    String.valueOf(now.toEpochMilli())
            );
            if (json == null) return null;
            T job = objectMapper.readValue(json, type);
            log.debug("지연 큐에서 실행 가능 작업 가져옴: queue={}", queueName);
            return job;
        } catch (Exception e) {
            log.warn("지연 큐 역직렬화 실패: queue={}, error={}", queueName, e.getMessage());
            return null;
        }
    }
}
