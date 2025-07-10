package com.byeolnight.controller.admin;

import com.byeolnight.dto.ai.NewsApiResponseDto;
import com.byeolnight.service.crawler.NewsDataService;
import com.byeolnight.service.crawler.SpaceNewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "👮 관리자 - 뉴스 관리", description = "뉴스 수집 및 관리 (NewsData.io API)")
@RestController
@RequestMapping("/api/admin/news-test")
@RequiredArgsConstructor
@Slf4j
public class NewsTestController {
    
    private final NewsDataService newsDataService;
    private final SpaceNewsService spaceNewsService;
    
    @Operation(summary = "NewsData.io API 테스트", description = "한국어 우주 뉴스를 가져와서 확인합니다")
    @GetMapping("/fetch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NewsApiResponseDto> testFetchNews() {
        log.info("NewsData.io API 테스트 시작");
        
        NewsApiResponseDto response = newsDataService.fetchKoreanSpaceNews();
        
        if (response != null) {
            log.info("뉴스 수집 성공: {}개", response.getResults().size());
            return ResponseEntity.ok(response);
        } else {
            log.warn("뉴스 수집 실패");
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(summary = "뉴스 수집 및 저장", description = "한국어 우주 뉴스를 수집하고 데이터베이스에 저장합니다")
    @PostMapping("/collect")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> collectAndSaveNews() {
        log.info("관리자 수동 뉴스 수집 시작");
        
        try {
            spaceNewsService.collectAndSaveSpaceNews();
            return ResponseEntity.ok("✅ 뉴스 수집 및 저장이 완료되었습니다!");
        } catch (Exception e) {
            log.error("뉴스 수집 중 오류 발생", e);
            return ResponseEntity.badRequest().body("❌ 뉴스 수집 실패: " + e.getMessage());
        }
    }
    
    @Operation(summary = "뉴스 수집 상태 확인", description = "마지막 뉴스 수집 정보를 확인합니다")
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getNewsCollectionStatus() {
        // 간단한 상태 정보 반환
        Map<String, Object> status = Map.of(
            "service", "NewsData.io API",
            "language", "한국어 (ko)",
            "keywords", "우주 OR 천문학 OR NASA OR 스페이스X OR 화성 OR 달 OR 위성 OR 항공우주",
            "maxResults", 10,
            "status", "활성화"
        );
        
        return ResponseEntity.ok(status);
    }
}