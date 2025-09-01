package com.byeolnight.domain.weather.service;

import com.byeolnight.domain.weather.dto.WeatherResponse;
import com.byeolnight.domain.weather.entity.WeatherObservation;
import com.byeolnight.domain.weather.repository.WeatherObservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
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
    
    public WeatherResponse getObservationConditions(Double latitude, Double longitude) {
        // 최근 1시간 내 데이터 확인
        Optional<WeatherObservation> recent = weatherRepository.findRecentByLocation(
            latitude, longitude, LocalDateTime.now().minusHours(1)
        );
        
        if (recent.isPresent()) {
            return convertToResponse(recent.get());
        }
        
        // 새로운 데이터 수집
        return fetchAndSaveWeatherData(latitude, longitude);
    }
    
    private WeatherResponse fetchAndSaveWeatherData(Double latitude, Double longitude) {
        try {
            String url = String.format("%s/weather?lat=%f&lon=%f&appid=%s&units=metric", 
                                     apiUrl, latitude, longitude, apiKey);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null) {
                WeatherObservation observation = parseWeatherData(response, latitude, longitude);
                weatherRepository.save(observation);
                return convertToResponse(observation);
            }
        } catch (Exception e) {
            log.error("날씨 데이터 수집 실패: {}", e.getMessage());
        }
        
        // 기본값 반환
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
    
    private WeatherObservation parseWeatherData(Map<String, Object> data, Double lat, Double lon) {
        Map<String, Object> main = (Map<String, Object>) data.get("main");
        Map<String, Object> clouds = (Map<String, Object>) data.get("clouds");
        
        double cloudCover = clouds != null ? ((Number) clouds.get("all")).doubleValue() : 50.0;
        double visibility = data.containsKey("visibility") ? 
            ((Number) data.get("visibility")).doubleValue() / 1000.0 : 10.0;
        
        String quality = calculateObservationQuality(cloudCover, visibility);
        
        return WeatherObservation.builder()
            .location(data.get("name").toString())
            .latitude(lat)
            .longitude(lon)
            .cloudCover(cloudCover)
            .visibility(visibility)
            .moonPhase("신월") // 간단한 기본값
            .observationQuality(quality)
            .observationTime(LocalDateTime.now())
            .build();
    }
    
    private String calculateObservationQuality(double cloudCover, double visibility) {
        if (cloudCover < 20 && visibility > 15) return "EXCELLENT";
        if (cloudCover < 40 && visibility > 10) return "GOOD";
        if (cloudCover < 70 && visibility > 5) return "FAIR";
        return "POOR";
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
}