package com.byeolnight.domain.weather.controller;

import com.byeolnight.domain.weather.dto.AstronomyEventResponse;
import com.byeolnight.domain.weather.dto.WeatherResponse;
import com.byeolnight.domain.weather.service.AstronomyService;
import com.byeolnight.domain.weather.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
@Tag(name = "날씨 및 천체 관측", description = "실시간 날씨 기반 별 관측 조건 및 천체 이벤트 API")
public class WeatherController {
    
    private final WeatherService weatherService;
    private final AstronomyService astronomyService;
    
    @GetMapping("/observation")
    @Operation(summary = "별 관측 조건 조회", 
               description = "위도/경도 기반으로 현재 별 관측에 적합한 날씨 조건을 조회합니다.")
    public ResponseEntity<WeatherResponse> getObservationConditions(
            @Parameter(description = "위도", example = "37.5665") 
            @RequestParam Double latitude,
            @Parameter(description = "경도", example = "126.9780") 
            @RequestParam Double longitude) {
        
        WeatherResponse response = weatherService.getObservationConditions(latitude, longitude);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/events")
    @Operation(summary = "예정된 천체 이벤트 조회", 
               description = "유성우, 일식/월식, 행성 근접 등 예정된 천체 이벤트 목록을 조회합니다.")
    public ResponseEntity<List<AstronomyEventResponse>> getUpcomingEvents() {
        List<AstronomyEventResponse> events = astronomyService.getUpcomingEvents();
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/iss")
    @Operation(summary = "ISS 실시간 위치 조회", 
               description = "국제우주정거장의 실시간 위치 정보를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getIssLocation() {
        try {
            Map<String, Object> issData = astronomyService.getIssLocation();
            return ResponseEntity.ok(issData);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "ISS 데이터 조회 실패"));
        }
    }
}