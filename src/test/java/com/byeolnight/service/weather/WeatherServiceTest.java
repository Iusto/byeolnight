package com.byeolnight.service.weather;

import com.byeolnight.dto.external.weather.OpenWeatherResponse;
import com.byeolnight.dto.weather.WeatherResponse;
import com.byeolnight.infrastructure.common.CacheResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WeatherService 테스트")
class WeatherServiceTest {

    private WeatherService weatherService;

    @Mock
    private WeatherLocalCacheService localCacheService;

    @Mock
    private RestTemplate restTemplate;

    private MeterRegistry meterRegistry;

    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_API_URL = "https://api.openweathermap.org/data/2.5";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        weatherService = new WeatherService(localCacheService, meterRegistry);
        ReflectionTestUtils.setField(weatherService, "apiKey", TEST_API_KEY);
        ReflectionTestUtils.setField(weatherService, "apiUrl", TEST_API_URL);
        ReflectionTestUtils.setField(weatherService, "restTemplate", restTemplate);
    }

    @Nested
    @DisplayName("별관측 날씨 조회")
    class GetObservationConditions {

        @Test
        @DisplayName("캐시에 데이터가 있으면 캐시에서 반환")
        void shouldReturnFromCacheWhenAvailable() {
            // given
            double latitude = 37.5665;
            double longitude = 126.9780;
            WeatherResponse cachedResponse = WeatherResponse.builder()
                    .location("서울")
                    .latitude(latitude)
                    .longitude(longitude)
                    .cloudCover(30.0)
                    .visibility(10.0)
                    .moonPhase("🌕")
                    .observationQuality("GOOD")
                    .recommendation("GOOD")
                    .observationTime("2026-01-05 12:00")
                    .build();

            given(localCacheService.get(anyString())).willReturn(Optional.of(cachedResponse));

            // when
            CacheResult<WeatherResponse> cacheResult = weatherService.getObservationConditions(latitude, longitude);

            // then
            assertThat(cacheResult).isNotNull();
            assertThat(cacheResult.cacheHit()).isTrue();
            WeatherResponse result = cacheResult.data();
            assertThat(result.getLocation()).isEqualTo("서울");
            assertThat(result.getCloudCover()).isEqualTo(30.0);
            verify(localCacheService, times(1)).get(anyString());
            verify(restTemplate, never()).getForObject(anyString(), eq(OpenWeatherResponse.class));
        }

        @Test
        @DisplayName("서울 좌표로 날씨 조회 - 캐시 미스 시 API 호출")
        void shouldFetchFromAPIForSeoulWhenCacheMiss() {
            // given
            double latitude = 37.5665;
            double longitude = 126.9780;
            // 0.01 단위로 반올림된 좌표
            double expectedLat = 37.57;
            double expectedLon = 126.98;

            OpenWeatherResponse apiResponse = createMockApiResponse("Seoul", 20, 10000);

            given(localCacheService.get(anyString())).willReturn(Optional.empty());
            given(restTemplate.getForObject(anyString(), eq(OpenWeatherResponse.class))).willReturn(apiResponse);

            // when
            CacheResult<WeatherResponse> cacheResult = weatherService.getObservationConditions(latitude, longitude);

            // then
            assertThat(cacheResult).isNotNull();
            assertThat(cacheResult.cacheHit()).isFalse();
            WeatherResponse result = cacheResult.data();
            assertThat(result.getLocation()).isEqualTo("Seoul");
            assertThat(result.getLatitude()).isEqualTo(expectedLat);
            assertThat(result.getLongitude()).isEqualTo(expectedLon);
            assertThat(result.getCloudCover()).isEqualTo(20.0);
            assertThat(result.getVisibility()).isEqualTo(10.0);
            verify(localCacheService, times(1)).put(anyString(), any(WeatherResponse.class));
        }

        @Test
        @DisplayName("부산 좌표로 날씨 조회 - 캐시 미스 시 API 호출")
        void shouldFetchFromAPIForBusanWhenCacheMiss() {
            // given
            double latitude = 35.1796;
            double longitude = 129.0756;
            // 0.01 단위로 반올림된 좌표
            double expectedLat = 35.18;
            double expectedLon = 129.08;

            OpenWeatherResponse apiResponse = createMockApiResponse("Busan", 40, 8000);

            given(localCacheService.get(anyString())).willReturn(Optional.empty());
            given(restTemplate.getForObject(anyString(), eq(OpenWeatherResponse.class))).willReturn(apiResponse);

            // when
            CacheResult<WeatherResponse> cacheResult = weatherService.getObservationConditions(latitude, longitude);

            // then
            assertThat(cacheResult).isNotNull();
            assertThat(cacheResult.cacheHit()).isFalse();
            WeatherResponse result = cacheResult.data();
            assertThat(result.getLocation()).isEqualTo("Busan");
            assertThat(result.getLatitude()).isEqualTo(expectedLat);
            assertThat(result.getLongitude()).isEqualTo(expectedLon);
            assertThat(result.getCloudCover()).isEqualTo(40.0);
            assertThat(result.getVisibility()).isEqualTo(8.0);
        }

        @Test
        @DisplayName("제주 좌표로 날씨 조회 - 캐시 미스 시 API 호출")
        void shouldFetchFromAPIForJejuWhenCacheMiss() {
            // given
            double latitude = 33.4996;
            double longitude = 126.5312;
            // 0.01 단위로 반올림된 좌표
            double expectedLat = 33.50;
            double expectedLon = 126.53;

            OpenWeatherResponse apiResponse = createMockApiResponse("Jeju", 60, 5000);

            given(localCacheService.get(anyString())).willReturn(Optional.empty());
            given(restTemplate.getForObject(anyString(), eq(OpenWeatherResponse.class))).willReturn(apiResponse);

            // when
            CacheResult<WeatherResponse> cacheResult = weatherService.getObservationConditions(latitude, longitude);

            // then
            assertThat(cacheResult).isNotNull();
            assertThat(cacheResult.cacheHit()).isFalse();
            WeatherResponse result = cacheResult.data();
            assertThat(result.getLocation()).isEqualTo("Jeju");
            assertThat(result.getLatitude()).isEqualTo(expectedLat);
            assertThat(result.getLongitude()).isEqualTo(expectedLon);
            assertThat(result.getCloudCover()).isEqualTo(60.0);
            assertThat(result.getVisibility()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("API 호출 실패 시 Fallback 응답 반환")
        void shouldReturnFallbackWhenAPIFails() {
            // given
            double latitude = 37.5665;
            double longitude = 126.9780;

            given(localCacheService.get(anyString())).willReturn(Optional.empty());
            given(restTemplate.getForObject(anyString(), eq(OpenWeatherResponse.class)))
                    .willThrow(new RuntimeException("API 호출 실패"));

            // when
            CacheResult<WeatherResponse> cacheResult = weatherService.getObservationConditions(latitude, longitude);

            // then
            assertThat(cacheResult).isNotNull();
            assertThat(cacheResult.cacheHit()).isFalse();
            WeatherResponse result = cacheResult.data();
            assertThat(result.getLocation()).isEqualTo("알 수 없음");
            assertThat(result.getObservationQuality()).isEqualTo("UNKNOWN");
            assertThat(result.getCloudCover()).isEqualTo(50.0);
            assertThat(result.getVisibility()).isEqualTo(10.0);
        }

        @Test
        @DisplayName("여러 다른 위치의 날씨 조회가 각각 올바르게 동작")
        void shouldHandleMultipleLocationsCorrectly() {
            // given
            double[] latitudes = {37.5665, 35.1796, 33.4996, 36.3504};
            double[] longitudes = {126.9780, 129.0756, 126.5312, 127.3845};
            // 0.01 단위로 반올림된 좌표
            double[] expectedLats = {37.57, 35.18, 33.50, 36.35};
            double[] expectedLons = {126.98, 129.08, 126.53, 127.38};
            String[] locations = {"Seoul", "Busan", "Jeju", "Daejeon"};

            given(localCacheService.get(anyString())).willReturn(Optional.empty());
            given(restTemplate.getForObject(anyString(), eq(OpenWeatherResponse.class))).willAnswer(invocation -> {
                String url = invocation.getArgument(0);
                for (int i = 0; i < locations.length; i++) {
                    if (url.contains(String.format("lat=%f", expectedLats[i]))) {
                        return createMockApiResponse(locations[i], 20 + (i * 10), 10000);
                    }
                }
                return createMockApiResponse("Unknown", 50, 10000);
            });

            // when & then
            for (int i = 0; i < locations.length; i++) {
                CacheResult<WeatherResponse> cacheResult = weatherService.getObservationConditions(latitudes[i], longitudes[i]);

                assertThat(cacheResult).isNotNull();
                WeatherResponse result = cacheResult.data();
                assertThat(result.getLocation()).isEqualTo(locations[i]);
                assertThat(result.getLatitude()).isCloseTo(expectedLats[i], org.assertj.core.data.Offset.offset(0.0001));
                assertThat(result.getLongitude()).isCloseTo(expectedLons[i], org.assertj.core.data.Offset.offset(0.0001));
            }
        }

        @Test
        @DisplayName("좌표 반올림이 적용되어 캐시 키가 생성됨")
        void shouldRoundCoordinatesForCacheKey() {
            // given
            // 37.564와 37.565는 0.01 단위로 반올림하면 같은 37.56이 되어야 함
            double latitude1 = 37.564;
            double latitude2 = 37.565;
            double longitude = 126.98;

            WeatherResponse cachedResponse = WeatherResponse.builder()
                    .location("서울")
                    .latitude(latitude1)
                    .longitude(longitude)
                    .cloudCover(30.0)
                    .visibility(10.0)
                    .moonPhase("🌕")
                    .observationQuality("GOOD")
                    .recommendation("GOOD")
                    .observationTime("2026-01-05 12:00")
                    .build();

            given(localCacheService.get(anyString())).willReturn(Optional.of(cachedResponse));

            // when
            CacheResult<WeatherResponse> result1 = weatherService.getObservationConditions(latitude1, longitude);
            CacheResult<WeatherResponse> result2 = weatherService.getObservationConditions(latitude2, longitude);

            // then
            // 두 결과 모두 캐시에서 가져와야 함
            assertThat(result1).isNotNull();
            assertThat(result1.cacheHit()).isTrue();
            assertThat(result2).isNotNull();
            assertThat(result2.cacheHit()).isTrue();
            verify(localCacheService, atLeastOnce()).get(anyString());
        }
    }

    private static OpenWeatherResponse createMockApiResponse(String name, int cloudCover, int visibility) {
        Map<String, Object> data = Map.of(
                "name", name,
                "clouds", Map.of("all", cloudCover),
                "visibility", visibility
        );
        return objectMapper.convertValue(data, OpenWeatherResponse.class);
    }
}
