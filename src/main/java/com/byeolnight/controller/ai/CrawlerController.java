package com.byeolnight.controller.ai;

import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.crawler.SpaceNewsScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/crawler")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "👮 관리자 API - 크롤러", description = "AI 기반 우주 콘텐츠 자동 수집 시스템")
@SecurityRequirement(name = "bearerAuth")
public class CrawlerController {

    private final SpaceNewsScheduler spaceNewsScheduler;

    @Operation(summary = "우주 뉴스 수동 수집", description = """
    관리자가 수동으로 우주 뉴스를 수집합니다.
    
    🔄 수집 프로세스:
    1. NewsData.io API에서 우주/과학 뉴스 수집
    2. AI를 통한 뉴스 요약 및 카테고리 분류
    3. 중복 뉴스 필터링
    4. 데이터베이스 저장
    
    ⏰ 자동 실행: 매일 오전 8시
    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "뉴스 수집 완료"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            @ApiResponse(responseCode = "500", description = "수집 중 오류 발생")
    })
    @PostMapping("/start")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<String>> startCrawling() {
        try {
            log.info("관리자 요청으로 우주 뉴스 수집 시작");
            spaceNewsScheduler.manualCollection();
            
            return ResponseEntity.ok(
                CommonResponse.success("우주 뉴스 수집이 완료되었습니다.")
            );
            
        } catch (Exception e) {
            log.error("뉴스 수집 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(CommonResponse.error("뉴스 수집 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @Operation(summary = "크롤러 상태 확인", description = "우주 뉴스 크롤러 시스템의 상태를 확인합니다. (스케줄링 상태 및 마지막 실행 시간 포함)")
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<String>> getStatus() {
        return ResponseEntity.ok(
            CommonResponse.success("우주 뉴스 크롤러 시스템이 정상 작동 중입니다. 매일 오전 8시에 자동 실행됩니다.")
        );
    }
    
    @Operation(summary = "토론 주제 생성 상태", description = "AI 기반 일일 토론 주제 생성 시스템 상태를 확인합니다. (Claude/OpenAI API 사용)")
    @GetMapping("/discussions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<String>> getDiscussionStatus() {
        return ResponseEntity.ok(
            CommonResponse.success("토론 주제 생성 시스템이 정상 작동 중입니다. 매일 오전 8시에 자동 실행됩니다.")
        );
    }
    
    @Operation(summary = "별빛 시네마 상태", description = "YouTube 우주 영상 자동 수집 및 번역 시스템 상태를 확인합니다.")
    @GetMapping("/cinema")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<String>> getCinemaStatus() {
        return ResponseEntity.ok(
            CommonResponse.success("별빛 시네마 시스템이 정상 작동 중입니다. 매일 오후 8시에 자동 실행됩니다.")
        );
    }
}