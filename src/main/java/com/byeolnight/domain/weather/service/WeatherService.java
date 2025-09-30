package com.byeolnight.domain.weather.service;

import com.byeolnight.domain.weather.dto.WeatherResponse;
import com.byeolnight.domain.weather.entity.WeatherObservation;
import com.byeolnight.domain.weather.repository.WeatherObservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {
    
    private final WeatherObservationRepository weatherRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${weather.api.key}")
    private String apiKey;
    
    @Value("${weather.api.url:https://api.openweathermap.org/data/2.5}")
    private String apiUrl;
    
    private static final int CACHE_HOURS = 1;
    
    public WeatherResponse getObservationConditions(Double latitude, Double longitude) {
        // 캐시된 데이터 확인
        Optional<WeatherObservation> cached = findCachedWeatherData(latitude, longitude);
        if (cached.isPresent()) {
            return convertToResponse(cached.get());
        }
        
        // 새로운 데이터 수집
        return fetchAndSaveWeatherData(latitude, longitude);
    }
    
    private Optional<WeatherObservation> findCachedWeatherData(Double latitude, Double longitude) {
        LocalDateTime cacheThreshold = LocalDateTime.now().minusHours(CACHE_HOURS);
        return weatherRepository.findRecentByLocation(latitude, longitude, cacheThreshold);
    }
    
    private WeatherResponse fetchAndSaveWeatherData(Double latitude, Double longitude) {
        try {
            Map<String, Object> apiResponse = callWeatherAPI(latitude, longitude);
            WeatherObservation observation = parseAndSaveWeatherData(apiResponse, latitude, longitude);
            return convertToResponse(observation);
            
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
    
    private WeatherObservation parseAndSaveWeatherData(Map<String, Object> apiData, Double lat, Double lon) {
        WeatherData weatherData = extractWeatherData(apiData);
        String quality = calculateObservationQuality(weatherData.cloudCover(), weatherData.visibility());
        
        WeatherObservation observation = WeatherObservation.builder()
            .location(weatherData.location())
            .latitude(lat)
            .longitude(lon)
            .cloudCover(weatherData.cloudCover())
            .visibility(weatherData.visibility())
            .moonPhase(getMoonPhaseIcon())
            .observationQuality(quality)
            .observationTime(LocalDateTime.now())
            .build();
            
        return weatherRepository.save(observation);
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
    
    private WeatherResponse convertToResponse(WeatherObservation observation) {
        String recommendation = generateRecommendation(observation.getObservationQuality());
        
        return WeatherResponse.builder()
            .location(observation.getLocation())
            .latitude(observation.getLatitude())
            .longitude(observation.getLongitude())
            .cloudCover(observation.getCloudCover())
            .visibility(observation.getVisibility())
            .moonPhase(observation.getMoonPhase())
            .observationQuality(observation.getObservationQuality())
            .recommendation(recommendation)
            .observationTime(observation.getObservationTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
            .build();
    }
    
    private String generateRecommendation(String quality) {
        return quality; // 프론트엔드에서 i18n 키로 사용
    }
    
    // 내부 데이터 클래스
    private record WeatherData(String location, double cloudCover, double visibility) {}
}