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
    
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Seoul") // 매일 오전 8시 (한국 시간)
    public void scheduleSpaceNewsCollection() {
        log.info("=== 우주 뉴스 자동 수집 스케줄 시작 - {} ===", java.time.LocalDateTime.now());
        
        try {
            if (spaceNewsService == null) {
                log.error("SpaceNewsService가 주입되지 않았습니다!");
                return;
            }
            
            spaceNewsService.collectAndSaveSpaceNews();
            log.info("=== 우주 뉴스 자동 수집 스케줄 완료 - {} ===", java.time.LocalDateTime.now());
        } catch (Exception e) {
            log.error("우주 뉴스 수집 중 오류 발생 - {}", java.time.LocalDateTime.now(), e);
        }
    }
    
    // 테스트용 수동 실행 메서드 (필요시 컨트롤러에서 호출)
    public void manualCollection() {
        log.info("=== 우주 뉴스 수동 수집 시작 ===");
        spaceNewsService.collectAndSaveSpaceNews();
        log.info("=== 우주 뉴스 수동 수집 완료 ===");
    }
    
    // 테스트용 - 2분마다 실행 (시간대 확인용)
    @Scheduled(fixedRate = 120000)
    public void testTimezoneScheduler() {
        log.info("테스트 스케줄러 - 서버 시간: {}, UTC: {}", 
            java.time.LocalDateTime.now(), 
            java.time.Instant.now());
    }
}