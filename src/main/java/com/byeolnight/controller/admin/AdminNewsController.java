package com.byeolnight.controller.admin;

import com.byeolnight.service.crawler.SpaceNewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "👮 관리자 - 뉴스 관리", description = "뉴스 수집 및 관리")
@RestController
@RequestMapping("/api/admin/news")
@RequiredArgsConstructor
@Slf4j
public class AdminNewsController {
    
    private final SpaceNewsService spaceNewsService;
    
    @Operation(summary = "뉴스 수동 수집", description = "관리자가 수동으로 우주 뉴스를 수집합니다")
    @PostMapping("/collect")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> collectNews() {
        log.info("관리자 수동 뉴스 수집 요청");
        
        try {
            spaceNewsService.collectAndSaveSpaceNews();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "🚀 뉴스 수집이 완료되었습니다!"
            ));
        } catch (Exception e) {
            log.error("뉴스 수집 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "❌ 뉴스 수집 실패: " + e.getMessage()
            ));
        }
    }
    
    @Operation(summary = "뉴스 수집 상태", description = "뉴스 수집 서비스 상태를 확인합니다")
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getNewsStatus() {
        Map<String, Object> status = Map.of(
            "service", "NewsData.io API",
            "language", "한국어 (ko)",
            "keywords", "우주 OR 천문학 OR NASA OR 스페이스X OR 화성 OR 달 OR 위성 OR 항공우주",
            "maxResults", 10,
            "status", "활성화",
            "description", "한국어 우주 관련 뉴스를 자동 수집합니다"
        );
        
        return ResponseEntity.ok(status);
    }
}