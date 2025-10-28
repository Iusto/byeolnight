package com.byeolnight.controller.admin;

import com.byeolnight.domain.weather.service.AstronomyService;
import com.byeolnight.dto.admin.AstronomyCollectionResultDto;
import com.byeolnight.dto.admin.AstronomyCollectionStatusDto;
import com.byeolnight.dto.admin.AstronomyStatsDto;
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
    public ResponseEntity<AstronomyCollectionResultDto> manualCollectAstronomyEvents() {
        log.info("관리자 천체 이벤트 수동 수집 요청");
        
        AstronomyCollectionResultDto result = astronomyService.manualFetchAstronomyEvents();
        
        if (result.isSuccess()) {
            log.info("관리자 천체 이벤트 수동 수집 성공: {} 개", result.getAfterCount());
        } else {
            log.warn("관리자 천체 이벤트 수동 수집 실패: {}", result.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/stats")
    @Operation(summary = "천체 이벤트 통계 조회", 
               description = "현재 저장된 천체 이벤트 데이터의 통계 정보를 조회합니다.")
    public ResponseEntity<AstronomyStatsDto> getAstronomyEventStats() {
        log.info("관리자 천체 이벤트 통계 조회 요청");
        return ResponseEntity.ok(astronomyService.getAstronomyEventStats());
    }
    
    @GetMapping("/status")
    @Operation(summary = "천체 데이터 수집 상태 조회", 
               description = "천체 데이터 수집 시스템의 현재 상태를 조회합니다.")
    public ResponseEntity<AstronomyCollectionStatusDto> getCollectionStatus() {
        return ResponseEntity.ok(astronomyService.getCollectionStatus());
    }
}