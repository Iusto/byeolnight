package com.byeolnight.service.weather;

import com.byeolnight.dto.weather.WeatherResponse;
import com.byeolnight.infrastructure.cache.RedissonCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * WeatherCacheService 테스트
 * - Redis 캐시 저장/조회 테스트
 */
@ExtendWith(MockitoExtension.class)
class WeatherCacheServiceTest {

    @Mock
    private RedissonCacheService redissonCacheService;

    @InjectMocks
    private WeatherCacheService weatherCacheService;

    private String cacheKey;
    private WeatherResponse testResponse;

    @BeforeEach
    void setUp() {
        cacheKey = "wx:37.4:127.0";
        testResponse = WeatherResponse.builder()
                .location("Seoul")
                .latitude(37.4)
                .longitude(127.0)
                .cloudCover(20.0)
                .visibility(10.0)
                .moonPhase("🌕")
                .observationQuality("EXCELLENT")
                .recommendation("EXCELLENT")
                .observationTime("2024-01-15 14:30")
                .build();
    }

    @Test
    @DisplayName("캐시 저장 - 정상 케이스")
    void put_Success() {
        // Given
        Duration ttl = Duration.ofMinutes(15);

        // When
        weatherCacheService.put(cacheKey, testResponse, ttl);

        // Then
        verify(redissonCacheService, times(1)).set(eq(cacheKey), eq(testResponse), eq(ttl));
    }

    @Test
    @DisplayName("캐시 조회 - HIT")
    void get_CacheHit() {
        // Given
        when(redissonCacheService.get(cacheKey)).thenReturn(testResponse);

        // When
        Optional<WeatherResponse> result = weatherCacheService.get(cacheKey);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testResponse);
        verify(redissonCacheService, times(1)).get(cacheKey);
    }

    @Test
    @DisplayName("캐시 조회 - MISS")
    void get_CacheMiss() {
        // Given
        when(redissonCacheService.get(cacheKey)).thenReturn(null);

        // When
        Optional<WeatherResponse> result = weatherCacheService.get(cacheKey);

        // Then
        assertThat(result).isEmpty();
        verify(redissonCacheService, times(1)).get(cacheKey);
    }

    @Test
    @DisplayName("캐시 삭제 - 정상 케이스")
    void delete_Success() {
        // Given
        when(redissonCacheService.delete(cacheKey)).thenReturn(true);

        // When
        weatherCacheService.delete(cacheKey);

        // Then
        verify(redissonCacheService, times(1)).delete(cacheKey);
    }
}