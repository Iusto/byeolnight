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
            case 0 -> "New Moon";
            case 1, 2 -> "Waxing Crescent";
            case 3, 4 -> "First Quarter";
            case 5, 6 -> "Full Moon";
            default -> "Last Quarter";
        };
    }
    
    private WeatherResponse createFallbackResponse(Double latitude, Double longitude) {
        return WeatherResponse.builder()
            .location("Unknown")
            .latitude(latitude)
            .longitude(longitude)
            .cloudCover(50.0)
            .visibility(10.0)
            .moonPhase("Unknown")
            .observationQuality("UNKNOWN")
            .recommendation("Unable to check weather information.")
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
            case "EXCELLENT" -> "Excellent observation conditions. Telescope observation is recommended.";
            case "GOOD" -> "Good observation conditions. Constellation observation is recommended.";
            case "FAIR" -> "Fair observation conditions. Simple stargazing is possible.";
            case "POOR" -> "Observation conditions are difficult. Indoor activities are recommended.";
            default -> "Checking weather information.";
        };
    }
    
    // 내부 데이터 클래스
    private record WeatherData(String location, double cloudCover, double visibility) {}
}