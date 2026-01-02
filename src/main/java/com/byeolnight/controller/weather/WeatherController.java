package com.byeolnight.controller.weather;

import com.byeolnight.dto.weather.AstronomyEventResponse;
import com.byeolnight.dto.weather.IssObservationResponse;
import com.byeolnight.dto.weather.WeatherResponse;
import com.byeolnight.service.weather.AstronomyService;
import com.byeolnight.service.weather.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "날씨 및 천체 관측", description = "실시간 날씨 기반 별 관측 조건 및 천체 이벤트 API")
public class WeatherController {
    
    private final WeatherService weatherService;
    private final AstronomyService astronomyService;
    
    @GetMapping("/observation")
    @Operation(summary = "별 관측 조건 조회",
               description = "위도/경도 기반으로 현재 별 관측에 적합한 날씨 조건을 조회합니다.")
    public ResponseEntity<WeatherResponse> getObservationConditions(
            @Parameter(description = "위도 (-90 ~ 90)", example = "37.5665")
            @RequestParam Double latitude,
            @Parameter(description = "경도 (-180 ~ 180)", example = "126.9780")
            @RequestParam Double longitude) {

        // 위도/경도 유효성 검증
        if (latitude < -90 || latitude > 90) {
            log.warn("잘못된 위도 값: {}", latitude);
            return ResponseEntity.badRequest().build();
        }
        if (longitude < -180 || longitude > 180) {
            log.warn("잘못된 경도 값: {}", longitude);
            return ResponseEntity.badRequest().build();
        }

        try {
            WeatherResponse response = weatherService.getObservationConditions(latitude, longitude);
            return ResponseEntity.ok()
                    .header("Cache-Control", "public, max-age=600")
                    .body(response);
        } catch (Exception e) {
            log.error("날씨 데이터 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/events")
    @Operation(summary = "천체 이벤트 조회", 
               description = "NASA API 기반 천체 이벤트 목록을 조회합니다.")
    public ResponseEntity<List<AstronomyEventResponse>> getUpcomingEvents() {
        List<AstronomyEventResponse> events = astronomyService.getUpcomingEvents();
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/iss")
    @Operation(summary = "ISS 관측 기회 조회", 
               description = "사용자 위치 기반 ISS 관측 기회 정보를 조회합니다.")
    public ResponseEntity<IssObservationResponse> getIssObservationOpportunity(
            @Parameter(description = "위도 (-90 ~ 90)", example = "37.5665") 
            @RequestParam Double latitude,
            @Parameter(description = "경도 (-180 ~ 180)", example = "126.9780") 
            @RequestParam Double longitude) {
        
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            IssObservationResponse issData = astronomyService.getIssObservationOpportunity(latitude, longitude);
            return ResponseEntity.ok(issData);
        } catch (Exception e) {
            log.error("ISS 관측 정보 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}