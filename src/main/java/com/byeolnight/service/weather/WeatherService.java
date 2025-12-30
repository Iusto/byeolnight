package com.byeolnight.service.weather;

import com.byeolnight.dto.weather.WeatherResponse;
import com.byeolnight.infrastructure.util.CoordinateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final WeatherCacheService weatherCacheService;
    private final WeatherRateLimitService rateLimitService;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${weather.api.key}")
    private String apiKey;
    
    @Value("${weather.api.url:https://api.openweathermap.org/data/2.5}")
    private String apiUrl;

    public WeatherResponse getObservationConditions(Double latitude, Double longitude) {
        // 1. 좌표 반올림 & 캐시 키 생성
        String cacheKey = CoordinateUtils.generateCacheKey(latitude, longitude);

        // 2. Redis 캐시 확인
        Optional<WeatherResponse> cached = weatherCacheService.get(cacheKey);
        if (cached.isPresent()) {
            log.info("캐시 HIT: cacheKey={}", cacheKey);
            return cached.get();
        }

        // 3. 호출 제한 확인
        boolean acquired = rateLimitService.tryAcquire(Duration.ofSeconds(2));
        if (!acquired) {
            log.warn("호출 제한 초과: cacheKey={}", cacheKey);
            throw new IllegalStateException("호출 제한 초과. 잠시 후 다시 시도해주세요.");
        }

        try {
            // 4. 외부 API 호출
            WeatherResponse fresh = fetchWeatherData(latitude, longitude);

            // 5. Redis 저장 (15분 TTL + jitter)
            Duration ttl = Duration.ofMinutes(15)
                    .plusSeconds(ThreadLocalRandom.current().nextInt(-60, 61));
            weatherCacheService.put(cacheKey, fresh, ttl);
            log.info("캐시 저장: cacheKey={}, ttl={}초", cacheKey, ttl.toSeconds());

            return fresh;
        } finally {
            // 6. Semaphore 해제
            rateLimitService.release();
        }
    }
    
    /**
     * 외부 API 호출 (Redis 캐시용)
     */
    private WeatherResponse fetchWeatherData(Double latitude, Double longitude) {
        try {
            Map<String, Object> apiResponse = callWeatherAPI(latitude, longitude);
            WeatherData weatherData = extractWeatherData(apiResponse);
            String quality = calculateObservationQuality(weatherData.cloudCover(), weatherData.visibility());
            String moonPhase = getMoonPhaseIcon();

            return WeatherResponse.builder()
                    .location(weatherData.location())
                    .latitude(latitude)
                    .longitude(longitude)
                    .cloudCover(weatherData.cloudCover())
                    .visibility(weatherData.visibility())
                    .moonPhase(moonPhase)
                    .observationQuality(quality)
                    .recommendation(quality)
                    .observationTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .build();

        } catch (Exception e) {
            log.error("날씨 데이터 수집 실패: {}", e.getMessage());
            return createFallbackResponse(latitude, longitude);
        }
    }

    private Map<String, Object> callWeatherAPI(Double latitude, Double longitude) {
        String url = String.format(
                java.util.Locale.US,
                "%s/weather?lat=%f&lon=%f&appid=%s&units=metric",
                apiUrl, latitude, longitude, apiKey
        );
        
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null) {
            throw new RuntimeException("API 응답이 null입니다");
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    private WeatherData extractWeatherData(Map<String, Object> apiData) {
        String location = String.valueOf(apiData.getOrDefault("name", "알 수 없음"));
        Map<String, Object> clouds = (Map<String, Object>) apiData.get("clouds");

        double cloudCover = 50.0;
        if (clouds != null) {
            Object all = clouds.get("all");
            if (all instanceof Number n) cloudCover = n.doubleValue();
        }

        double visibilityKm = 10.0;
        Object vis = apiData.get("visibility");
        if (vis instanceof Number n) visibilityKm = n.doubleValue() / 1000.0;

        return new WeatherData(location, cloudCover, visibilityKm);
    }

    private String calculateObservationQuality(double cloudCover, double visibilityKm) {
        if (cloudCover < 20 && visibilityKm >= 8)  return "EXCELLENT"; // 8~10km
        if (cloudCover < 40 && visibilityKm >= 6)  return "GOOD";
        if (cloudCover < 70 && visibilityKm >= 3)  return "FAIR";
        return "POOR";
    }

    /**
     * UTC 시간을 율리우스력으로 변환
     * @param dtUtc UTC 기준 날짜시간
     * @return 율리우스 일수 (Julian Day Number)
     */
    private static double toJulian(LocalDateTime dtUtc) {
        int Y = dtUtc.getYear(), M = dtUtc.getMonthValue(), D = dtUtc.getDayOfMonth();
        int A = (14 - M) / 12;
        Y = Y + 4800 - A;
        M = M + 12 * A - 3;
        long JDN = D + (153L*M + 2)/5 + 365L*Y + Y/4 - Y/100 + Y/400 - 32045;
        double frac = (dtUtc.getHour() - 12) / 24.0 + dtUtc.getMinute()/1440.0 + dtUtc.getSecond()/86400.0;
        return JDN + frac;
    }

    /**
     * 달의 위상 비율 계산
     * @param nowUtc UTC 기준 현재 시간
     * @return 달의 위상 비율 (0.0~1.0, 0.5≈보름달)
     */
    private static double moonPhaseFraction(LocalDateTime nowUtc) {
        final double SYNODIC = 29.530588853;      // 회합월 (달의 위상 주기)
        final double NEWMOON_JDN = 2451550.1;     // 2000-01-06 18:14 UT 신월 기준점
        double j = toJulian(nowUtc);
        double cycles = (j - NEWMOON_JDN) / SYNODIC;
        return cycles - Math.floor(cycles); // 0.0~1.0 (0.5≈보름달)
    }

    /**
     * 달의 위상을 이모지 아이콘으로 반환
     * @param f 달의 위상 비율 (0.0~1.0, 0.5≈보름달)
     * @return 달의 위상 이모지
     */
    private static String getMoonPhase(double f) {
        if (f < 0.03 || f > 0.97) return "🌑"; // 신월
        if (f < 0.22)              return "🌒"; // 초승달
        if (Math.abs(f - 0.25) < 0.03) return "🌓"; // 상현달
        if (f < 0.47)              return "🌔"; // 상현→보름
        if (Math.abs(f - 0.50) < 0.03) return "🌕"; // 보름달
        if (f < 0.72)              return "🌖"; // 보름→하현
        if (Math.abs(f - 0.75) < 0.03) return "🌗"; // 하현달
        return "🌘"; // 그믐달
    }

    /**
     * 현재 달의 위상을 정확히 계산하여 이모지로 반환
     * @return 달의 위상 이모지
     */
    private String getMoonPhaseIcon() {
        LocalDateTime nowUtc = LocalDateTime.now(java.time.ZoneOffset.UTC);
        double f = moonPhaseFraction(nowUtc);
        return getMoonPhase(f);
    }

    private WeatherResponse createFallbackResponse(Double latitude, Double longitude) {
        return WeatherResponse.builder()
            .location("알 수 없음")
            .latitude(latitude)
            .longitude(longitude)
            .cloudCover(50.0)
            .visibility(10.0)
            .moonPhase("🌙")
            .observationQuality("UNKNOWN")
            .recommendation("UNKNOWN")
            .observationTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
            .build();
    }

    // 내부 데이터 클래스
    private record WeatherData(String location, double cloudCover, double visibility) {}
}