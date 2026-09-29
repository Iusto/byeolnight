package com.byeolnight.service.weather;

import com.byeolnight.dto.external.weather.OpenWeatherResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenWeatherClient 테스트")
class OpenWeatherClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private WeatherApiRateLimiter rateLimiter;

    private SimpleMeterRegistry meterRegistry;
    private OpenWeatherClient client;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        client = new OpenWeatherClient(restTemplate, rateLimiter, meterRegistry);
        ReflectionTestUtils.setField(client, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(client, "apiUrl", "https://api.openweathermap.org/data/2.5");
    }

    @Test
    @DisplayName("호출 허가를 받은 뒤 날씨 API를 호출하고 메트릭을 기록한다")
    void shouldAcquirePermitBeforeCallingApi() {
        OpenWeatherResponse response = new OpenWeatherResponse();
        given(rateLimiter.acquire()).willReturn(0.25);
        given(restTemplate.getForObject(
                eq("https://api.openweathermap.org/data/2.5/weather?lat=37.566500&lon=126.978000&appid=test-api-key&units=metric"),
                eq(OpenWeatherResponse.class)))
                .willReturn(response);

        OpenWeatherResponse result = client.fetch(37.5665, 126.9780);

        assertThat(result).isSameAs(response);
        verify(rateLimiter).acquire();
        assertThat(meterRegistry.counter("weather.api.request").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("weather.api.success").count()).isEqualTo(1.0);
        assertThat(meterRegistry.timer("weather.api.rate_limit.wait").count()).isEqualTo(1);
        assertThat(meterRegistry.timer("weather.api.rate_limit.wait").totalTime(java.util.concurrent.TimeUnit.SECONDS))
                .isEqualTo(0.25);
    }

    @Test
    @DisplayName("HTTP 429 응답을 별도 메트릭으로 기록한다")
    void shouldRecordTooManyRequests() {
        given(rateLimiter.acquire()).willReturn(0.0);
        given(restTemplate.getForObject(
                eq("https://api.openweathermap.org/data/2.5/weather?lat=37.566500&lon=126.978000&appid=test-api-key&units=metric"),
                eq(OpenWeatherResponse.class)))
                .willThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.fetch(37.5665, 126.9780))
                .isInstanceOf(HttpClientErrorException.class);

        assertThat(meterRegistry.counter("weather.api.failure").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("weather.api.http.429").count()).isEqualTo(1.0);
    }
}
