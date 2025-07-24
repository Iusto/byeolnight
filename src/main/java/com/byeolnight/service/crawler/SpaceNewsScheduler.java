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
    
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Seoul")
    public void scheduleSpaceNewsCollection() {
        collectNewsWithRetry();
    }
    
    // 5분 후 재시도
    @Scheduled(cron = "0 5 8 * * *", zone = "Asia/Seoul")
    public void retrySpaceNewsCollection() {
        if (shouldRetryNewsToday()) {
            log.info("우주 뉴스 재시도 시작");
            collectNewsWithRetry();
        }
    }
    
    // 10분 후 마지막 재시도
    @Scheduled(cron = "0 10 8 * * *", zone = "Asia/Seoul")
    public void finalRetrySpaceNewsCollection() {
        if (shouldRetryNewsToday()) {
            log.info("우주 뉴스 마지막 재시도 시작");
            collectNewsWithRetry();
        }
    }
    
    private void collectNewsWithRetry() {
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
    
    private boolean shouldRetryNewsToday() {
        // 오늘 이미 성공한 뉴스 게시글이 있는지 확인
        java.time.LocalDateTime todayStart = java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        java.time.LocalDateTime todayEnd = todayStart.plusDays(1);
        
        // SpaceNewsService에서 오늘 뉴스 개수 확인하는 메서드 필요
        boolean shouldRetry = spaceNewsService.getTodayNewsCount() == 0;
        log.info("오늘 뉴스 게시글 수: {}, 재시도 필요: {}", spaceNewsService.getTodayNewsCount(), shouldRetry);
        return shouldRetry;
    }
    
    // 테스트용 수동 실행 메서드 (필요시 컨트롤러에서 호출)
    public void manualCollection() {
        log.info("=== 우주 뉴스 수동 수집 시작 ===");
        spaceNewsService.collectAndSaveSpaceNews();
        log.info("=== 우주 뉴스 수동 수집 완료 ===");
    }
}