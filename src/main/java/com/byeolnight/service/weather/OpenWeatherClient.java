package com.byeolnight.service.weather;

import com.byeolnight.dto.external.weather.OpenWeatherResponse;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * OpenWeather 호출을 한곳으로 모아 호출 속도와 메트릭을 일관되게 관리한다.
 */
@Component
public class OpenWeatherClient {

    private final RestTemplate restTemplate;
    private final WeatherApiRateLimiter rateLimiter;
    private final MeterRegistry meterRegistry;

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.url:https://api.openweathermap.org/data/2.5}")
    private String apiUrl;

    public OpenWeatherClient(@Qualifier("weatherRestTemplate") RestTemplate restTemplate,
                             WeatherApiRateLimiter rateLimiter,
                             MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.rateLimiter = rateLimiter;
        this.meterRegistry = meterRegistry;
    }

    public OpenWeatherResponse fetch(double latitude, double longitude) {
        double waitSeconds = rateLimiter.acquire();
        meterRegistry.timer("weather.api.rate_limit.wait")
                .record(Math.round(waitSeconds * 1_000_000_000L), TimeUnit.NANOSECONDS);
        meterRegistry.counter("weather.api.request").increment();

        String url = String.format(
                Locale.US,
                "%s/weather?lat=%f&lon=%f&appid=%s&units=metric",
                apiUrl, latitude, longitude, apiKey
        );

        try {
            OpenWeatherResponse response = restTemplate.getForObject(url, OpenWeatherResponse.class);
            if (response == null) {
                throw new IllegalStateException("날씨 API 응답이 null입니다");
            }
            meterRegistry.counter("weather.api.success").increment();
            return response;
        } catch (RestClientResponseException e) {
            meterRegistry.counter("weather.api.failure").increment();
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                meterRegistry.counter("weather.api.http.429").increment();
            }
            throw e;
        } catch (RuntimeException e) {
            meterRegistry.counter("weather.api.failure").increment();
            throw e;
        }
    }
}
