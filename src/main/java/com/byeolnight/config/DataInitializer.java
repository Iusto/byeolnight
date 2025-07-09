package com.byeolnight.config;

import com.byeolnight.service.shop.StellaShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final StellaShopService stellaShopService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 아이콘 개수 확인 후 필요시 초기화
        long iconCount = stellaShopService.getIconCount();
        if (iconCount < 44) {
            log.info("스텔라 아이콘이 부족합니다. ({}/44) 자동 초기화를 시작합니다.", iconCount);
            stellaShopService.initializeDefaultIcons();
        } else {
            log.info("스텔라 아이콘이 이미 초기화되어 있습니다. ({}/44)", iconCount);
        }
    }
}