package com.byeolnight.service.weather;

import com.byeolnight.dto.weather.WeatherResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 로컬 메모리 기반 날씨 캐시 서비스
 * - ConcurrentHashMap 사용
 * - 30분마다 스케줄러가 갱신
 */
@Slf4j
@Service
public class WeatherLocalCacheService {

    private final Map<String, CachedWeather> cache = new ConcurrentHashMap<>();

    /**
     * 캐시에서 날씨 데이터 조회
     */
    public Optional<WeatherResponse> get(String cacheKey) {
        CachedWeather cached = cache.get(cacheKey);
        if (cached != null) {
            log.debug("로컬 캐시 HIT: cacheKey={}", cacheKey);
            return Optional.of(cached.data());
        }
        log.debug("로컬 캐시 MISS: cacheKey={}", cacheKey);
        return Optional.empty();
    }

    /**
     * 캐시에 날씨 데이터 저장
     */
    public void put(String cacheKey, WeatherResponse data) {
        cache.put(cacheKey, new CachedWeather(data, LocalDateTime.now()));
        log.info("로컬 캐시 저장: cacheKey={}, location={}", cacheKey, data.getLocation());
    }

    /**
     * 캐시 전체 삭제 (관리자용)
     */
    public void clear() {
        int size = cache.size();
        cache.clear();
        log.info("로컬 캐시 전체 삭제: {}개 항목", size);
    }

    /**
     * 캐시 통계
     */
    public Map<String, Object> getStats() {
        return Map.of(
                "size", cache.size(),
                "keys", cache.keySet()
        );
    }

    /**
     * 캐시된 날씨 데이터 (데이터 + 캐시 시간)
     */
    private record CachedWeather(WeatherResponse data, LocalDateTime cachedAt) {
    }
}
