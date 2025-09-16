package com.byeolnight.controller.admin;

import com.byeolnight.domain.weather.service.AstronomyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/astronomy")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "관리자 천체 이벤트", description = "관리자 전용 천체 이벤트 데이터 관리 API")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAstronomyController {
    
    private final AstronomyService astronomyService;
    
    @PostMapping("/collect")
    @Operation(summary = "천체 이벤트 수동 수집", 
               description = "관리자가 수동으로 NASA/KASI API에서 천체 이벤트 데이터를 수집합니다.")
    public ResponseEntity<Map<String, Object>> manualCollectAstronomyEvents() {
        try {
            log.info("관리자 천체 이벤트 수동 수집 요청");
            
            Map<String, Object> result = astronomyService.manualFetchAstronomyEvents();
            
            if ((Boolean) result.get("success")) {
                log.info("관리자 천체 이벤트 수동 수집 성공: {} 개", result.get("afterCount"));
                return ResponseEntity.ok(result);
            } else {
                log.warn("관리자 천체 이벤트 수동 수집 실패: {}", result.get("message"));
                return ResponseEntity.ok(result); // 실패해도 200으로 응답 (결과 정보 포함)
            }
            
        } catch (Exception e) {
            log.error("관리자 천체 이벤트 수동 수집 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", false,
                    "message", "서버 오류: " + e.getMessage(),
                    "error", e.getClass().getSimpleName()
                ));
        }
    }
    
    @GetMapping("/stats")
    @Operation(summary = "천체 이벤트 통계 조회", 
               description = "현재 저장된 천체 이벤트 데이터의 통계 정보를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getAstronomyEventStats() {
        try {
            log.info("관리자 천체 이벤트 통계 조회 요청");
            
            Map<String, Object> stats = astronomyService.getAstronomyEventStats();
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("관리자 천체 이벤트 통계 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "서버 오류: " + e.getMessage()
                ));
        }
    }
    
    @GetMapping("/status")
    @Operation(summary = "천체 데이터 수집 상태 조회", 
               description = "천체 데이터 수집 시스템의 현재 상태를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getCollectionStatus() {
        try {
            Map<String, Object> status = Map.of(
                "scheduledCollection", "매일 오전 9시 자동 실행",
                "manualCollection", "관리자 수동 실행 가능",
                "dataSources", Map.of(
                    "NASA_NeoWs", "지구 근접 소행성 데이터",
                    "NASA_DONKI", "태양 플레어, 지자기 폭풍 데이터", 
                    "KASI", "한국천문연구원 월식/일식/슈퍼문 데이터"
                ),
                "supportedEventTypes", Map.of(
                    "ASTEROID", "소행성 근접 통과",
                    "SOLAR_FLARE", "태양 플레어",
                    "GEOMAGNETIC_STORM", "지자기 폭풍",
                    "LUNAR_ECLIPSE", "월식",
                    "SOLAR_ECLIPSE", "일식",
                    "BLOOD_MOON", "개기월식",
                    "SUPERMOON", "슈퍼문",
                    "BLUE_MOON", "블루문"
                )
            );
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("천체 데이터 수집 상태 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "서버 오류: " + e.getMessage()));
        }
    }
}