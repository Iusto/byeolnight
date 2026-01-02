package com.byeolnight.service.weather;

import com.byeolnight.dto.weather.IssObservationResponse;

import lombok.RequiredArgsConstructor;
import lombok.Getter;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    private static final String ISS_LOCATION_URL = "https://api.wheretheiss.at/v1/satellites/25544";

    public IssObservationResponse getIssObservationOpportunity(double latitude, double longitude) {
        try {
            IssLocationData issData = fetchIssCurrentLocation();
            IssPassData nextPass = calculateNextIssPass(latitude, longitude);
            
            return IssObservationResponse.builder()
                .messageKey("iss.detailed_status")
                .friendlyMessage(createIssStatusMessage(issData))
                .currentAltitudeKm(issData != null ? issData.getAltitude() : null)
                .currentVelocityKmh(issData != null ? issData.getVelocity() : null)
                .nextPassTime(nextPass.getNextPassTime())
                .nextPassDate(nextPass.getNextPassDate())
                .nextPassDirection(nextPass.getNextPassDirection())
                .estimatedDuration(nextPass.getEstimatedDuration())
                .visibilityQuality(nextPass.getVisibilityQuality())
                .build();
            
        } catch (Exception e) {
            log.error("ISS 관측 정보 조회 실패: {}", e.getMessage());
            return createFallbackIssInfo();
        }
    }

    private IssLocationData fetchIssCurrentLocation() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ISS_LOCATION_URL))
                .timeout(Duration.ofSeconds(30))
                .build();
                
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode issData = objectMapper.readTree(response.body());
                return IssLocationData.builder()
                    .altitude(issData.get("altitude").asDouble())
                    .velocity(issData.get("velocity").asDouble())
                    .latitude(issData.get("latitude").asDouble())
                    .longitude(issData.get("longitude").asDouble())
                    .build();
            }
        } catch (Exception e) {
            log.warn("ISS 위치 조회 실패: {}", e.getMessage());
        }
        return null;
    }
    
    private IssPassData calculateNextIssPass(double latitude, double longitude) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextPass = now.plusHours(2).plusMinutes(30);
        
        return IssPassData.builder()
            .nextPassTime(nextPass.format(DateTimeFormatter.ofPattern("HH:mm")))
            .nextPassDate(nextPass.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
            .nextPassDirection("NORTHEAST")
            .estimatedDuration("5-7분")
            .visibilityQuality("GOOD")
            .build();
    }
    
    private String createIssStatusMessage(IssLocationData issData) {
        if (issData == null) {
            return "ISS는 현재 지구 상공 약 400km에서 시속 27,600km로 이동 중입니다.";
        }
        
        return String.format("ISS는 현재 고도 %.0fkm에서 시속 %.0fkm로 이동 중입니다.", 
                           issData.getAltitude(), issData.getVelocity());
    }
    
    private IssObservationResponse createFallbackIssInfo() {
        LocalDateTime nextPass = LocalDateTime.now().plusHours(3);
        
        return IssObservationResponse.builder()
            .messageKey("iss.fallback")
            .friendlyMessage("ISS는 지구 상공 400km에서 90분마다 지구를 한 바퀴 돕니다.")
            .currentAltitudeKm(408.0)
            .currentVelocityKmh(27600.0)
            .nextPassTime(nextPass.format(DateTimeFormatter.ofPattern("HH:mm")))
            .nextPassDate(nextPass.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
            .nextPassDirection("NORTHEAST")
            .estimatedDuration("5-7분")
            .visibilityQuality("GOOD")
            .build();
    }
    
    @Getter
    @Builder
    private static class IssLocationData {
        private final Double altitude;
        private final Double velocity;
        private final Double latitude;
        private final Double longitude;
    }
    
    @Getter
    @Builder
    private static class IssPassData {
        private final String nextPassTime;
        private final String nextPassDate;
        private final String nextPassDirection;
        private final String estimatedDuration;
        private final String visibilityQuality;
    }
}
