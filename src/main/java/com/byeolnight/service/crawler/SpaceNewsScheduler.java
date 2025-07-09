package com.byeolnight.service.crawler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpaceNewsScheduler {
    
    private final SpaceNewsService spaceNewsService;
    
    @Scheduled(cron = "0 0 8 * * *") // 매일 오전 8시
    public void scheduleSpaceNewsCollection() {
        log.info("=== 우주 뉴스 자동 수집 스케줄 시작 ===");
        
        try {
            spaceNewsService.collectAndSaveSpaceNews();
            log.info("=== 우주 뉴스 자동 수집 스케줄 완료 ===");
        } catch (Exception e) {
            log.error("우주 뉴스 수집 중 오류 발생", e);
        }
    }
    
    // 테스트용 수동 실행 메서드 (필요시 컨트롤러에서 호출)
    public void manualCollection() {
        log.info("=== 우주 뉴스 수동 수집 시작 ===");
        spaceNewsService.collectAndSaveSpaceNews();
        log.info("=== 우주 뉴스 수동 수집 완료 ===");
    }
}