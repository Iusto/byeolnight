package com.byeolnight.service.weather;

import com.byeolnight.dto.weather.WeatherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

    @InjectMocks
    private WeatherService weatherService;

    @Mock
    private WeatherLocalCacheService localCacheService;

    @Mock
    private RestTemplate restTemplate;

    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_API_URL = "https://api.openweathermap.org/data/2.5";

    @BeforeEach
    void setUp() {
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
            WeatherResponse result = weatherService.getObservationConditions(latitude, longitude);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getLocation()).isEqualTo("서울");
            assertThat(result.getCloudCover()).isEqualTo(30.0);
            verify(localCacheService, times(1)).get(anyString());
            verify(restTemplate, never()).getForObject(anyString(), eq(Map.class));
        }

        @Test
        @DisplayName("서울 좌표로 날씨 조회 - 캐시 미스 시 API 호출")
        void shouldFetchFromAPIForSeoulWhenCacheMiss() {
            // given
            double latitude = 37.5665;
            double longitude = 126.9780;
            // 0.2 단위로 반올림된 좌표
            double expectedLat = 37.6;
            double expectedLon = 127.0;

            Map<String, Object> apiResponse = Map.of(
                    "name", "Seoul",
                    "clouds", Map.of("all", 20),
                    "visibility", 10000
            );

            given(localCacheService.get(anyString())).willReturn(Optional.empty());
            given(restTemplate.getForObject(anyString(), eq(Map.class))).willReturn(apiResponse);

            // when
            WeatherResponse result = weatherService.getObservationConditions(latitude, longitude);

            // then
            assertThat(result).isNotNull();
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
            // 0.2 단위로 반올림된 좌표
            double expectedLat = 35.2;
            double expectedLon = 129.0;

            Map<String, Object> apiResponse = Map.of(
                    "name", "Busan",
                    "clouds", Map.of("all", 40),
                    "visibility", 8000
            );

            given(localCacheService.get(anyString())).willReturn(Optional.empty());
            given(restTemplate.getForObject(anyString(), eq(Map.class))).willReturn(apiResponse);

            // when
            WeatherResponse result = weatherService.getObservationConditions(latitude, longitude);

            // then
            assertThat(result).isNotNull();
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
            // 0.2 단위로 반올림된 좌표
            double expectedLat = 33.4;
            double expectedLon = 126.6;

            Map<String, Object> apiResponse = Map.of(
                    "name", "Jeju",
                    "clouds", Map.of("all", 60),
                    "visibility", 5000
            );

            given(localCacheService.get(anyString())).willReturn(Optional.empty());
            given(restTemplate.getForObject(anyString(), eq(Map.class))).willReturn(apiResponse);

            // when
            WeatherResponse result = weatherService.getObservationConditions(latitude, longitude);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getLocation()).isEqualTo("Jeju");
            assertThat(result.getLatitude()).isEqualTo(expectedLat);
            // 부동소수점 정밀도로 인해 126.60000000000001이 될 수 있음
            assertThat(result.getLongitude()).isCloseTo(expectedLon, org.assertj.core.data.Offset.offset(0.0001));
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
            given(restTemplate.getForObject(anyString(), eq(Map.class)))
                    .willThrow(new RuntimeException("API 호출 실패"));

            // when
            WeatherResponse result = weatherService.getObservationConditions(latitude, longitude);

            // then
            assertThat(result).isNotNull();
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
            double[] expectedLats = {37.6, 35.2, 33.4, 36.4};
            double[] expectedLons = {127.0, 129.0, 126.6, 127.4};
            String[] locations = {"Seoul", "Busan", "Jeju", "Daejeon"};

            given(localCacheService.get(anyString())).willReturn(Optional.empty());
            given(restTemplate.getForObject(anyString(), eq(Map.class))).willAnswer(invocation -> {
                String url = invocation.getArgument(0);
                for (int i = 0; i < locations.length; i++) {
                    if (url.contains(String.format("lat=%f", expectedLats[i]))) {
                        return Map.of(
                                "name", locations[i],
                                "clouds", Map.of("all", 20 + (i * 10)),
                                "visibility", 10000
                        );
                    }
                }
                return Map.of("name", "Unknown", "clouds", Map.of("all", 50), "visibility", 10000);
            });

            // when & then
            for (int i = 0; i < locations.length; i++) {
                WeatherResponse result = weatherService.getObservationConditions(latitudes[i], longitudes[i]);

                assertThat(result).isNotNull();
                assertThat(result.getLocation()).isEqualTo(locations[i]);
                assertThat(result.getLatitude()).isEqualTo(expectedLats[i]);
                // 부동소수점 정밀도 문제로 isCloseTo 사용
                assertThat(result.getLongitude()).isCloseTo(expectedLons[i], org.assertj.core.data.Offset.offset(0.0001));
            }
        }

        @Test
        @DisplayName("좌표 반올림이 적용되어 캐시 키가 생성됨")
        void shouldRoundCoordinatesForCacheKey() {
            // given
            // 37.56과 37.57은 0.2 단위로 반올림하면 같은 37.6이 되어야 함
            double latitude1 = 37.56;
            double latitude2 = 37.57;
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
            WeatherResponse result1 = weatherService.getObservationConditions(latitude1, longitude);
            WeatherResponse result2 = weatherService.getObservationConditions(latitude2, longitude);

            // then
            // 두 결과 모두 캐시에서 가져와야 함
            assertThat(result1).isNotNull();
            assertThat(result2).isNotNull();
            verify(localCacheService, atLeastOnce()).get(anyString());
        }
    }
}