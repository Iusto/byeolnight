package com.byeolnight.service.weather;

import com.byeolnight.config.WeatherCityConfig;
import com.byeolnight.dto.external.weather.OpenWeatherResponse;
import com.byeolnight.dto.weather.WeatherResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WeatherScheduler 테스트")
class WeatherSchedulerTest {

    private WeatherScheduler weatherScheduler;

    @Mock
    private WeatherLocalCacheService cacheService;

    @Mock
    private WeatherCityConfig cityConfig;

    @Mock
    private OpenWeatherClient openWeatherClient;

    private SimpleMeterRegistry meterRegistry;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        weatherScheduler = new WeatherScheduler(
                cacheService, cityConfig, openWeatherClient, meterRegistry,
                new ObservationScoreService(), millis -> { });
    }

    @Test
    @DisplayName("운영 생성자가 Spring 주입 대상으로 명시됨")
    void shouldDeclareAutowiredConstructor() {
        assertThat(WeatherScheduler.class.getConstructors())
                .anyMatch(constructor -> constructor.isAnnotationPresent(Autowired.class));
    }

    @Test
    @DisplayName("WeatherCityConfig에 정의된 도시들만 캐싱")
    void shouldCacheOnlyConfiguredCities() {
        // given
        List<WeatherCityConfig.City> testCities = List.of(
                new WeatherCityConfig.City("서울", 37.5665, 126.9780),
                new WeatherCityConfig.City("부산", 35.1796, 129.0756),
                new WeatherCityConfig.City("제주", 33.4996, 126.5312)
        );

        given(cityConfig.getCities()).willReturn(testCities);

        // Mock API 응답
        given(openWeatherClient.fetch(anyDouble(), anyDouble())).willAnswer(invocation -> {
            double latitude = invocation.getArgument(0);
            if (latitude == 37.5665) {
                return createMockOpenWeatherResponse("Seoul", 20, 10000);
            } else if (latitude == 35.1796) {
                return createMockOpenWeatherResponse("Busan", 30, 9000);
            } else if (latitude == 33.4996) {
                return createMockOpenWeatherResponse("Jeju", 40, 8000);
            }
            return createMockOpenWeatherResponse("Unknown", 50, 10000);
        });

        // when
        weatherScheduler.collectWeatherData();

        // then
        // 정확히 3개 도시에 대해서만 캐시 저장이 호출되어야 함
        ArgumentCaptor<WeatherResponse> weatherCaptor = ArgumentCaptor.forClass(WeatherResponse.class);
        verify(cacheService, times(3)).put(anyString(), weatherCaptor.capture());

        List<WeatherResponse> cachedWeathers = weatherCaptor.getAllValues();
        assertThat(cachedWeathers).hasSize(3);

        // 캐시된 도시 이름 확인
        List<String> cachedCityNames = cachedWeathers.stream()
                .map(WeatherResponse::getLocation)
                .toList();

        assertThat(cachedCityNames).containsExactlyInAnyOrder("서울", "부산", "제주");
    }

    @Test
    @DisplayName("실제 WeatherCityConfig의 모든 도시 캐싱")
    void shouldCacheAllConfiguredCities() {
        // given
        WeatherCityConfig realConfig = new WeatherCityConfig();
        ReflectionTestUtils.setField(weatherScheduler, "cityConfig", realConfig);

        // Mock API 응답
        given(openWeatherClient.fetch(anyDouble(), anyDouble()))
                .willReturn(createMockOpenWeatherResponse("TestCity", 30, 10000));

        // when
        weatherScheduler.collectWeatherData();

        // then
        // WeatherCityConfig에 정의된 도시 수만큼 캐시 저장이 호출되어야 함
        int expectedCityCount = realConfig.getCities().size();
        verify(cacheService, times(expectedCityCount)).put(anyString(), any(WeatherResponse.class));
    }

    @Test
    @DisplayName("각 도시마다 별도의 캐시 키로 저장")
    void shouldUseSeparateCacheKeysForEachCity() {
        // given
        List<WeatherCityConfig.City> testCities = List.of(
                new WeatherCityConfig.City("서울", 37.5665, 126.9780),
                new WeatherCityConfig.City("부산", 35.1796, 129.0756)
        );

        given(cityConfig.getCities()).willReturn(testCities);
        given(openWeatherClient.fetch(anyDouble(), anyDouble()))
                .willReturn(createMockOpenWeatherResponse("TestCity", 30, 10000));

        // when
        weatherScheduler.collectWeatherData();

        // then
        ArgumentCaptor<String> cacheKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(cacheService, times(2)).put(cacheKeyCaptor.capture(), any(WeatherResponse.class));

        List<String> cacheKeys = cacheKeyCaptor.getAllValues();
        assertThat(cacheKeys).hasSize(2);
        assertThat(cacheKeys.get(0)).isNotEqualTo(cacheKeys.get(1));

        // 캐시 키 형식 확인 (wx:위도:경도)
        assertThat(cacheKeys.get(0)).matches("wx:\\d+\\.\\d+:\\d+\\.\\d+");
        assertThat(cacheKeys.get(1)).matches("wx:\\d+\\.\\d+:\\d+\\.\\d+");
    }

    @Test
    @DisplayName("API 호출이 재시도 후에도 실패하면 기존 캐시를 덮어쓰지 않는다")
    void shouldKeepExistingCacheWhenApiFailsAfterRetry() {
        // given
        List<WeatherCityConfig.City> testCities = List.of(
                new WeatherCityConfig.City("서울", 37.5665, 126.9780),
                new WeatherCityConfig.City("부산", 35.1796, 129.0756)
        );

        given(cityConfig.getCities()).willReturn(testCities);

        given(openWeatherClient.fetch(eq(37.5665), eq(126.9780)))
                .willThrow(new RuntimeException("API 호출 실패"));
        given(openWeatherClient.fetch(eq(35.1796), eq(129.0756)))
                .willReturn(createMockOpenWeatherResponse("Busan", 30, 10000));

        // when
        weatherScheduler.collectWeatherData();

        // then
        // 실패한 서울은 저장하지 않고, 성공한 부산만 갱신한다.
        ArgumentCaptor<WeatherResponse> weatherCaptor = ArgumentCaptor.forClass(WeatherResponse.class);
        verify(cacheService, times(1)).put(anyString(), weatherCaptor.capture());

        List<WeatherResponse> cachedWeathers = weatherCaptor.getAllValues();
        WeatherResponse busanWeather = cachedWeathers.get(0);
        assertThat(busanWeather.getLocation()).isEqualTo("부산");
        assertThat(busanWeather.getObservationQuality()).isNotEqualTo("UNKNOWN");
        assertThat(busanWeather.getObservationScore()).isBetween(0, 100);
        assertThat(busanWeather.getDataStatus()).isEqualTo(WeatherResponse.DataStatus.FRESH);

        verify(openWeatherClient, times(2)).fetch(37.5665, 126.9780);
        assertThat(meterRegistry.counter("weather.scheduler.refresh.retry").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("weather.scheduler.refresh.failure").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("weather.scheduler.refresh.success").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("수집된 날씨 데이터에 한글 도시 이름 포함")
    void shouldIncludeKoreanCityNames() {
        // given
        List<WeatherCityConfig.City> testCities = List.of(
                new WeatherCityConfig.City("서울", 37.5665, 126.9780),
                new WeatherCityConfig.City("부산", 35.1796, 129.0756)
        );

        given(cityConfig.getCities()).willReturn(testCities);
        given(openWeatherClient.fetch(anyDouble(), anyDouble()))
                .willReturn(createMockOpenWeatherResponse("Seoul", 30, 10000));

        // when
        weatherScheduler.collectWeatherData();

        // then
        ArgumentCaptor<WeatherResponse> weatherCaptor = ArgumentCaptor.forClass(WeatherResponse.class);
        verify(cacheService, times(2)).put(anyString(), weatherCaptor.capture());

        List<WeatherResponse> cachedWeathers = weatherCaptor.getAllValues();

        // API 응답의 영문 이름이 아닌 Config의 한글 이름이 사용되어야 함
        assertThat(cachedWeathers.get(0).getLocation()).isEqualTo("서울");
        assertThat(cachedWeathers.get(1).getLocation()).isEqualTo("부산");
    }

    private OpenWeatherResponse createMockOpenWeatherResponse(String name, int cloudCover, int visibility) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("clouds", Map.of("all", cloudCover));
        data.put("visibility", visibility);
        return objectMapper.convertValue(data, OpenWeatherResponse.class);
    }
}
