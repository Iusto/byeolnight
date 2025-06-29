package com.byeolnight.controller.admin;

import com.byeolnight.config.CrawlerConfig;
import com.byeolnight.dto.ai.NewsDto;
import com.byeolnight.dto.ai.EventDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.crawler.CrawlerService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/crawler")
@RequiredArgsConstructor
@Slf4j
public class CrawlerController {

    private final CrawlerService crawlerService;
    private final CrawlerConfig crawlerConfig;

    @Operation(summary = "FastAPI에서 뉴스 데이터 수신", description = "FastAPI 크롤러에서 전송된 뉴스 데이터를 받아 게시글로 등록합니다.")
    @PostMapping("/news")
    public ResponseEntity<CommonResponse<String>> receiveNews(
            @RequestHeader(value = "X-Crawler-API-Key", required = false) String apiKey,
            @RequestBody NewsDto newsDto) {
        try {
            log.info("뉴스 데이터 수신: {}", newsDto.getTitle());
            
            // 뉴스 API 키 검증
            if (!crawlerConfig.isValidNewsApiKey(apiKey)) {
                log.warn("잘못된 뉴스 API 키로 접근 시도: {}", apiKey);
                return ResponseEntity.status(401)
                    .body(CommonResponse.error("유효하지 않은 뉴스 API 키입니다."));
            }
            
            // 필수 필드 검증
            if (newsDto.getTitle() == null || newsDto.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(CommonResponse.error("뉴스 제목이 필요합니다."));
            }
            
            if (newsDto.getContent() == null || newsDto.getContent().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(CommonResponse.error("뉴스 내용이 필요합니다."));
            }
            
            // 뉴스 데이터 처리
            crawlerService.processNewsData(newsDto);
            
            return ResponseEntity.ok(
                CommonResponse.success("뉴스 게시글이 성공적으로 등록되었습니다.")
            );
            
        } catch (Exception e) {
            log.error("뉴스 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(CommonResponse.error("뉴스 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @Operation(summary = "이벤트 데이터 수신", description = "FastAPI 크롤러에서 전송된 이벤트 데이터를 받아 게시글로 등록합니다.")
    @PostMapping("/events")
    public ResponseEntity<CommonResponse<String>> receiveEvent(
            @RequestHeader(value = "X-Crawler-API-Key", required = false) String apiKey,
            @RequestBody EventDto eventDto) {
        try {
            log.info("이벤트 데이터 수신: {}", eventDto.getTitle());
            
            // 이벤트 API 키 검증
            if (!crawlerConfig.isValidEventApiKey(apiKey)) {
                log.warn("잘못된 이벤트 API 키로 접근 시도: {}", apiKey);
                return ResponseEntity.status(401)
                    .body(CommonResponse.error("유효하지 않은 이벤트 API 키입니다."));
            }
            
            // 필수 필드 검증
            if (eventDto.getTitle() == null || eventDto.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(CommonResponse.error("이벤트 제목이 필요합니다."));
            }
            
            // 기본 정보 중 하나라도 있어야 함
            if ((eventDto.getContent() == null || eventDto.getContent().trim().isEmpty()) &&
                (eventDto.getProgramName() == null || eventDto.getProgramName().trim().isEmpty()) &&
                (eventDto.getLocation() == null || eventDto.getLocation().trim().isEmpty())) {
                return ResponseEntity.badRequest()
                    .body(CommonResponse.error("이벤트 내용, 프로그램명, 또는 위치 정보 중 하나 이상이 필요합니다."));
            }
            
            // 이벤트 데이터 처리
            crawlerService.processEventData(eventDto);
            
            return ResponseEntity.ok(
                CommonResponse.success("이벤트 게시글이 성공적으로 등록되었습니다.")
            );
            
        } catch (Exception e) {
            log.error("이벤트 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(CommonResponse.error("이벤트 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @Operation(summary = "뉴스 데이터 상태 확인", description = "크롤러 시스템의 상태를 확인합니다.")
    @GetMapping("/status")
    public ResponseEntity<CommonResponse<String>> getStatus() {
        return ResponseEntity.ok(
            CommonResponse.success("크롤러 시스템이 정상 작동 중입니다.")
        );
    }
}