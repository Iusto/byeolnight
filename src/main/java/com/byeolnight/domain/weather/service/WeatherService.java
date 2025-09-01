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
    
    @Value("${weather.api.key:dummy-key}")
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
        String url = String.format("%s/weather?lat=%f&lon=%f&appid=%s&units=metric", 
                                 apiUrl, latitude, longitude, apiKey);
        
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
            .moonPhase(getMoonPhase()) // 간단한 계산
            .observationQuality(quality)
            .observationTime(LocalDateTime.now())
            .build();
            
        return weatherRepository.save(observation);
    }
    
    private WeatherData extractWeatherData(Map<String, Object> apiData) {
        String location = apiData.getOrDefault("name", "알 수 없음").toString();
        
        Map<String, Object> clouds = (Map<String, Object>) apiData.get("clouds");
        double cloudCover = clouds != null ? ((Number) clouds.get("all")).doubleValue() : 50.0;
        
        double visibility = apiData.containsKey("visibility") ? 
            ((Number) apiData.get("visibility")).doubleValue() / 1000.0 : 10.0;
            
        return new WeatherData(location, cloudCover, visibility);
    }
    
    private String calculateObservationQuality(double cloudCover, double visibility) {
        if (cloudCover < 20 && visibility > 15) return "EXCELLENT";
        if (cloudCover < 40 && visibility > 10) return "GOOD";
        if (cloudCover < 70 && visibility > 5) return "FAIR";
        return "POOR";
    }
    
    private String getMoonPhase() {
        // 간단한 달의 위상 계산 (실제로는 더 복잡한 계산 필요)
        int dayOfMonth = LocalDateTime.now().getDayOfMonth();
        return switch (dayOfMonth % 8) {
            case 0 -> "신월";
            case 1, 2 -> "초승달";
            case 3, 4 -> "상현달";
            case 5, 6 -> "보름달";
            default -> "하현달";
        };
    }
    
    private WeatherResponse createFallbackResponse(Double latitude, Double longitude) {
        return WeatherResponse.builder()
            .location("알 수 없음")
            .latitude(latitude)
            .longitude(longitude)
            .cloudCover(50.0)
            .visibility(10.0)
            .moonPhase("알 수 없음")
            .observationQuality("UNKNOWN")
            .recommendation("날씨 정보를 확인할 수 없습니다.")
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
        return switch (quality) {
            case "EXCELLENT" -> "🌟 완벽한 관측 조건입니다! 망원경을 준비하세요.";
            case "GOOD" -> "⭐ 좋은 관측 조건입니다. 별 관측을 추천합니다.";
            case "FAIR" -> "🌤️ 보통 조건입니다. 밝은 별들을 관측할 수 있습니다.";
            case "POOR" -> "☁️ 관측이 어려운 조건입니다. 실내 활동을 추천합니다.";
            default -> "날씨 정보를 확인 중입니다.";
        };
    }
    
    // 내부 데이터 클래스
    private record WeatherData(String location, double cloudCover, double visibility) {}
}