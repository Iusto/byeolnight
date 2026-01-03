package com.byeolnight.service.weather;

import com.byeolnight.dto.weather.WeatherResponse;
import com.byeolnight.infrastructure.util.CoordinateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

/**
 * 날씨 서비스
 * - 로컬 캐시에서 날씨 데이터 제공
 * - 30분마다 스케줄러가 주요 도시 날씨 자동 수집
 * - 캐시에 없으면 실시간 API 호출
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final WeatherLocalCacheService localCacheService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.url:https://api.openweathermap.org/data/2.5}")
    private String apiUrl;

    /**
     * 별관측 날씨 조회
     * - 로컬 캐시에서 조회 (스케줄러가 30분마다 갱신)
     * - 캐시에 없으면 실시간 API 호출 후 캐시 저장
     */
    public WeatherResponse getObservationConditions(Double latitude, Double longitude) {
        // 좌표 반올림 & 캐시 키 생성
        double roundedLat = CoordinateUtils.roundCoordinate(latitude);
        double roundedLon = CoordinateUtils.roundCoordinate(longitude);
        String cacheKey = CoordinateUtils.generateCacheKey(latitude, longitude);

        // 로컬 캐시 확인
        Optional<WeatherResponse> cached = localCacheService.get(cacheKey);
        if (cached.isPresent()) {
            log.debug("캐시에서 날씨 반환: cacheKey={}", cacheKey);
            return cached.get();
        }

        // 캐시에 없으면 실시간 API 호출
        log.info("캐시 MISS: cacheKey={} - 실시간 API 호출", cacheKey);
        try {
            WeatherResponse realTimeData = fetchWeatherDataFromAPI(roundedLat, roundedLon);
            localCacheService.put(cacheKey, realTimeData);
            return realTimeData;
        } catch (Exception e) {
            log.error("실시간 날씨 API 호출 실패: lat={}, lon={}, error={}", latitude, longitude, e.getMessage());
            return createFallbackResponse(latitude, longitude);
        }
    }

    /**
     * 외부 API에서 실시간 날씨 데이터 조회
     */
    private WeatherResponse fetchWeatherDataFromAPI(double latitude, double longitude) {
        Map<String, Object> apiResponse = callWeatherAPI(latitude, longitude);
        WeatherData weatherData = extractWeatherData(apiResponse);
        String locationName = extractLocationName(apiResponse);
        String quality = calculateObservationQuality(weatherData.cloudCover(), weatherData.visibility());
        String moonPhase = getMoonPhaseIcon();

        return WeatherResponse.builder()
                .location(locationName)
                .latitude(latitude)
                .longitude(longitude)
                .cloudCover(weatherData.cloudCover())
                .visibility(weatherData.visibility())
                .moonPhase(moonPhase)
                .observationQuality(quality)
                .recommendation(quality)
                .observationTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .build();
    }

    /**
     * OpenWeatherMap API 호출
     */
    private Map<String, Object> callWeatherAPI(double latitude, double longitude) {
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

    /**
     * API 응답에서 날씨 데이터 추출
     */
    @SuppressWarnings("unchecked")
    private WeatherData extractWeatherData(Map<String, Object> apiData) {
        Map<String, Object> clouds = (Map<String, Object>) apiData.get("clouds");

        double cloudCover = 50.0;
        if (clouds != null) {
            Object all = clouds.get("all");
            if (all instanceof Number n) cloudCover = n.doubleValue();
        }

        double visibilityKm = 10.0;
        Object vis = apiData.get("visibility");
        if (vis instanceof Number n) visibilityKm = n.doubleValue() / 1000.0;

        return new WeatherData(cloudCover, visibilityKm);
    }

    /**
     * API 응답에서 지역명 추출
     */
    private String extractLocationName(Map<String, Object> apiData) {
        Object name = apiData.get("name");
        if (name instanceof String locationName && !locationName.isEmpty()) {
            return locationName;
        }
        return "알 수 없음";
    }

    /**
     * 별 관측 품질 계산
     */
    private String calculateObservationQuality(double cloudCover, double visibilityKm) {
        if (cloudCover < 20 && visibilityKm >= 8) return "EXCELLENT";
        if (cloudCover < 40 && visibilityKm >= 6) return "GOOD";
        if (cloudCover < 70 && visibilityKm >= 3) return "FAIR";
        return "POOR";
    }

    /**
     * 달의 위상 아이콘 계산
     */
    private String getMoonPhaseIcon() {
        LocalDateTime nowUtc = LocalDateTime.now(java.time.ZoneOffset.UTC);
        double f = moonPhaseFraction(nowUtc);
        return getMoonPhase(f);
    }

    private static double toJulian(LocalDateTime dtUtc) {
        int Y = dtUtc.getYear(), M = dtUtc.getMonthValue(), D = dtUtc.getDayOfMonth();
        int A = (14 - M) / 12;
        Y = Y + 4800 - A;
        M = M + 12 * A - 3;
        long JDN = D + (153L * M + 2) / 5 + 365L * Y + Y / 4 - Y / 100 + Y / 400 - 32045;
        double frac = (dtUtc.getHour() - 12) / 24.0 + dtUtc.getMinute() / 1440.0 + dtUtc.getSecond() / 86400.0;
        return JDN + frac;
    }

    private static double moonPhaseFraction(LocalDateTime nowUtc) {
        final double SYNODIC = 29.530588853;
        final double NEWMOON_JDN = 2451550.1;
        double j = toJulian(nowUtc);
        double cycles = (j - NEWMOON_JDN) / SYNODIC;
        return cycles - Math.floor(cycles);
    }

    private static String getMoonPhase(double f) {
        if (f < 0.03 || f > 0.97) return "🌑";
        if (f < 0.22) return "🌒";
        if (Math.abs(f - 0.25) < 0.03) return "🌓";
        if (f < 0.47) return "🌔";
        if (Math.abs(f - 0.50) < 0.03) return "🌕";
        if (f < 0.72) return "🌖";
        if (Math.abs(f - 0.75) < 0.03) return "🌗";
        return "🌘";
    }

    /**
     * Fallback 응답 생성
     * - API 호출이 실패했을 때만 사용
     */
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

    private record WeatherData(double cloudCover, double visibility) {
    }
}
