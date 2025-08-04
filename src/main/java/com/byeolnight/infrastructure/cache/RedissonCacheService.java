package com.byeolnight.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
}