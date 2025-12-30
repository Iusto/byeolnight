package com.byeolnight.service.weather;

import com.byeolnight.dto.weather.WeatherResponse;
import com.byeolnight.infrastructure.cache.RedissonCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 날씨 데이터 Redis 캐시 서비스
 * - 좌표 반올림된 캐시 키로 저장
 * - 15분 TTL + 지터 적용
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class WeatherCacheService {

    private final RedissonCacheService redissonCacheService;

    /**
     * 캐시에서 날씨 데이터 조회
     */
    public Optional<WeatherResponse> get(String cacheKey) {
        WeatherResponse cached = redissonCacheService.get(cacheKey);
        if (cached != null) {
            log.info("Cache HIT: cacheKey={}", cacheKey);
            return Optional.of(cached);
        }
        log.info("Cache MISS: cacheKey={}", cacheKey);
        return Optional.empty();
    }

    /**
     * 캐시에 날씨 데이터 저장
     */
    public void put(String cacheKey, WeatherResponse data, Duration ttl) {
        redissonCacheService.set(cacheKey, data, ttl);
        log.info("Cache PUT: cacheKey={}, ttl={}초", cacheKey, ttl.toSeconds());
    }

    /**
     * 캐시 삭제 (관리자용)
     */
    public void delete(String cacheKey) {
        boolean deleted = redissonCacheService.delete(cacheKey);
        log.info("Cache DELETE: cacheKey={}, deleted={}", cacheKey, deleted);
    }
}