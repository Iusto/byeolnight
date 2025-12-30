package com.byeolnight.service.weather;

import com.byeolnight.dto.weather.WeatherResponse;
import com.byeolnight.infrastructure.util.CoordinateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherService 통합 테스트")
class WeatherServiceIntegrationTest {

    @Mock
    private WeatherCacheService weatherCacheService;

    @Mock
    private WeatherRateLimitService rateLimitService;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(weatherCacheService, rateLimitService);
    }

    @Test
    @DisplayName("캐시 HIT 시 외부 API 호출하지 않음")
    void cacheHit_shouldNotCallExternalAPI() {
        // given
        double lat = 37.4979;  // 강남
        double lon = 127.0276;
        String cacheKey = CoordinateUtils.generateCacheKey(lat, lon);

        WeatherResponse cachedData = WeatherResponse.builder()
                .location("Seoul")
                .latitude(lat)
                .longitude(lon)
                .cloudCover(20.0)
                .visibility(10.0)
                .moonPhase("🌕")
                .observationQuality("EXCELLENT")
                .recommendation("EXCELLENT")
                .observationTime("2024-01-15 14:30")
                .build();

        // Mock: Redis 캐시에서 데이터 반환
        when(weatherCacheService.get(cacheKey)).thenReturn(Optional.of(cachedData));

        // when
        WeatherResponse result = weatherService.getObservationConditions(lat, lon);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getLocation()).isEqualTo("Seoul");
        assertThat(result.getCloudCover()).isEqualTo(20.0);

        // 캐시에서 조회했는지 확인
        verify(weatherCacheService, times(1)).get(cacheKey);

        // 호출 제한이나 외부 API는 호출되지 않아야 함
        verify(rateLimitService, never()).tryAcquire(any(Duration.class));
    }

    @Test
    @DisplayName("같은 그리드 내 좌표들은 같은 캐시 키를 공유한다")
    void sameGrid_shouldShareCacheKey() {
        // given
        double lat1 = 37.45;  // 강남 근처 (37.4 그리드)
        double lon1 = 127.05; // (127.0 그리드)

        double lat2 = 37.49;  // 조금 다른 위치지만 같은 그리드
        double lon2 = 127.09; // (127.0 그리드)

        String cacheKey1 = CoordinateUtils.generateCacheKey(lat1, lon1);
        String cacheKey2 = CoordinateUtils.generateCacheKey(lat2, lon2);

        // then - 같은 그리드 좌표들은 같은 캐시 키를 가져야 함
        assertThat(cacheKey1).isEqualTo(cacheKey2);
        assertThat(cacheKey1).isEqualTo("wx:37.4:127.0");

        WeatherResponse cachedData = WeatherResponse.builder()
                .location("Seoul")
                .latitude(lat1)
                .longitude(lon1)
                .cloudCover(20.0)
                .visibility(10.0)
                .moonPhase("🌕")
                .observationQuality("EXCELLENT")
                .recommendation("EXCELLENT")
                .observationTime("2024-01-15 14:30")
                .build();

        // Mock: 첫 번째 좌표로 캐시 설정
        when(weatherCacheService.get(cacheKey1)).thenReturn(Optional.of(cachedData));

        // when - 두 번째 좌표로 조회
        WeatherResponse result = weatherService.getObservationConditions(lat2, lon2);

        // then - 같은 캐시를 사용하므로 데이터를 반환해야 함
        assertThat(result).isNotNull();
        assertThat(result.getLocation()).isEqualTo("Seoul");

        // 같은 캐시 키로 조회했는지 확인
        verify(weatherCacheService, times(1)).get(cacheKey2);
    }

    @Test
    @DisplayName("호출 제한 초과 시 예외가 발생한다")
    void rateLimitExceeded_shouldThrowException() {
        // given
        double lat = 37.4979;  // 강남
        double lon = 127.0276;
        String cacheKey = CoordinateUtils.generateCacheKey(lat, lon);

        // Mock: 캐시 MISS
        when(weatherCacheService.get(cacheKey)).thenReturn(Optional.empty());

        // Mock: 호출 제한 초과
        when(rateLimitService.tryAcquire(any(Duration.class))).thenReturn(false);

        // when & then - 예외가 발생해야 함
        try {
            weatherService.getObservationConditions(lat, lon);
            org.junit.jupiter.api.Assertions.fail("예외가 발생해야 합니다");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).contains("호출 제한 초과");
        }

        // 호출 제한 확인이 호출되었는지 검증
        verify(rateLimitService, times(1)).tryAcquire(any(Duration.class));

        // Semaphore는 획득하지 못했으므로 release 호출되지 않아야 함
        verify(rateLimitService, never()).release();
    }
}