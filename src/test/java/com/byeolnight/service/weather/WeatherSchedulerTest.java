package com.byeolnight.service.weather;

import com.byeolnight.config.WeatherCityConfig;
import com.byeolnight.dto.weather.WeatherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

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

    @InjectMocks
    private WeatherScheduler weatherScheduler;

    @Mock
    private WeatherLocalCacheService cacheService;

    @Mock
    private WeatherCityConfig cityConfig;

    @Mock
    private RestTemplate restTemplate;

    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_API_URL = "https://api.openweathermap.org/data/2.5";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(weatherScheduler, "apiKey", TEST_API_KEY);
        ReflectionTestUtils.setField(weatherScheduler, "apiUrl", TEST_API_URL);
        ReflectionTestUtils.setField(weatherScheduler, "restTemplate", restTemplate);
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
        given(restTemplate.getForObject(anyString(), eq(Map.class))).willAnswer(invocation -> {
            String url = invocation.getArgument(0);
            if (url.contains("lat=37.6")) {
                return createMockApiResponse("Seoul", 20, 10000);
            } else if (url.contains("lat=35.2")) {
                return createMockApiResponse("Busan", 30, 9000);
            } else if (url.contains("lat=33.4")) {
                return createMockApiResponse("Jeju", 40, 8000);
            }
            return createMockApiResponse("Unknown", 50, 10000);
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
        given(restTemplate.getForObject(anyString(), eq(Map.class)))
                .willReturn(createMockApiResponse("TestCity", 30, 10000));

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
        given(restTemplate.getForObject(anyString(), eq(Map.class)))
                .willReturn(createMockApiResponse("TestCity", 30, 10000));

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
    @DisplayName("API 호출 실패 시 Fallback 응답으로 캐시 저장")
    void shouldSaveFallbackResponseWhenApiFails() {
        // given
        List<WeatherCityConfig.City> testCities = List.of(
                new WeatherCityConfig.City("서울", 37.5665, 126.9780),
                new WeatherCityConfig.City("부산", 35.1796, 129.0756)
        );

        given(cityConfig.getCities()).willReturn(testCities);

        // 첫 번째 도시는 실패, 두 번째는 성공
        boolean[] firstCall = {true};
        given(restTemplate.getForObject(anyString(), eq(Map.class))).willAnswer(invocation -> {
            if (firstCall[0]) {
                firstCall[0] = false;
                throw new RuntimeException("API 호출 실패");
            }
            return createMockApiResponse("Busan", 30, 10000);
        });

        // when
        weatherScheduler.collectWeatherData();

        // then
        // 2개 도시 모두 캐시 저장 (실패한 도시는 Fallback)
        ArgumentCaptor<WeatherResponse> weatherCaptor = ArgumentCaptor.forClass(WeatherResponse.class);
        verify(cacheService, times(2)).put(anyString(), weatherCaptor.capture());

        List<WeatherResponse> cachedWeathers = weatherCaptor.getAllValues();

        // 첫 번째 도시는 Fallback 응답
        WeatherResponse seoulWeather = cachedWeathers.get(0);
        assertThat(seoulWeather.getLocation()).isEqualTo("서울");
        assertThat(seoulWeather.getObservationQuality()).isEqualTo("UNKNOWN");

        // 두 번째 도시는 정상 응답
        WeatherResponse busanWeather = cachedWeathers.get(1);
        assertThat(busanWeather.getLocation()).isEqualTo("부산");
        assertThat(busanWeather.getObservationQuality()).isNotEqualTo("UNKNOWN");
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
        given(restTemplate.getForObject(anyString(), eq(Map.class)))
                .willReturn(createMockApiResponse("Seoul", 30, 10000));

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

    private Map<String, Object> createMockApiResponse(String name, int cloudCover, int visibility) {
        Map<String, Object> response = new HashMap<>();
        response.put("name", name);
        response.put("clouds", Map.of("all", cloudCover));
        response.put("visibility", visibility);
        return response;
    }
}