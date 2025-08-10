package com.byeolnight.config;

import com.byeolnight.service.shop.StellaShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 데이터 초기화 통합 관리자
 * - 스텔라 아이콘 초기화
 * - 기타 데이터 초기화 작업
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final StellaShopService stellaShopService;
    private static boolean initialized = false;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (initialized) {
            log.info("데이터 초기화 이미 완료. 중복 실행 방지.");
            return;
        }
        
        try {
            log.info("데이터 초기화 시작");
            
            // 1. 스텔라 아이콘 초기화
            initializeStellaIcons();
            
            // 2. 기타 데이터 초기화 (향후 추가 가능)
            // initializeOtherData();
            
            initialized = true;
            log.info("데이터 초기화 완료");
            
        } catch (Exception e) {
            log.error("데이터 초기화 실패", e);
        }
    }
    
    private void initializeStellaIcons() {
        try {
            long iconCount = stellaShopService.getIconCount();
            if (iconCount < 44) {
                log.info("스텔라 아이콘 부족 ({}/44). 초기화 시작.", iconCount);
                stellaShopService.initializeDefaultIcons();
                log.info("스텔라 아이콘 초기화 완료");
            } else {
                log.info("스텔라 아이콘 충분 ({}/44)", iconCount);
            }
        } catch (Exception e) {
            log.error("스텔라 아이콘 초기화 실패", e);
        }
    }
}