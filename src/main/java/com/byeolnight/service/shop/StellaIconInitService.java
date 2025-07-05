package com.byeolnight.service.shop;

import com.byeolnight.domain.entity.shop.StellaIcon;
import com.byeolnight.domain.entity.shop.StellaIconGrade;
import com.byeolnight.domain.repository.shop.StellaIconRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StellaIconInitService {

    private final StellaIconRepository stellaIconRepository;

    @Transactional
    public void initializeIcons() {
        // 이미 데이터가 있으면 스킵
        if (stellaIconRepository.count() > 0) {
            log.info("스텔라 아이콘 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("스텔라 아이콘 초기 데이터를 생성합니다...");

        List<StellaIcon> icons = List.of(
            // COMMON 등급
            StellaIcon.builder()
                .name("반짝이는 별")
                .description("가장 기본적인 별 아이콘입니다.")
                .iconUrl("/icons/basic_star.svg")
                .price(0)
                .grade(StellaIconGrade.COMMON)
                .type(StellaIcon.IconType.STATIC)
                .available(true)
                .animationClass("")
                .build(),
            
            StellaIcon.builder()
                .name("반짝이는 유성")
                .description("처음 포인트를 획득하면 구매할 수 있습니다.")
                .iconUrl("/icons/shiny_meteor.svg")
                .price(50)
                .grade(StellaIconGrade.COMMON)
                .type(StellaIcon.IconType.ANIMATED)
                .available(true)
                .animationClass("animate-pulse")
                .build(),

            StellaIcon.builder()
                .name("픽셀 플래닛")
                .description("복고 감성의 도트 행성 아이콘")
                .iconUrl("/icons/twilight_planet.svg")
                .price(130)
                .grade(StellaIconGrade.COMMON)
                .type(StellaIcon.IconType.ANIMATED)
                .available(true)
                .animationClass("animate-spin")
                .build(),

            // RARE 등급
            StellaIcon.builder()
                .name("황혼의 행성")
                .description("글 10개 작성 달성 시 구매 가능.")
                .iconUrl("/icons/twilight_planet.svg")
                .price(100)
                .grade(StellaIconGrade.RARE)
                .type(StellaIcon.IconType.ANIMATED)
                .available(true)
                .animationClass("animate-pulse")
                .build(),

            StellaIcon.builder()
                .name("자정의 달")
                .description("7일 연속 활동 시 잠금 해제.")
                .iconUrl("/icons/midnight_moon.svg")
                .price(150)
                .grade(StellaIconGrade.RARE)
                .type(StellaIcon.IconType.ANIMATED)
                .available(true)
                .animationClass("animate-bounce")
                .build(),

            StellaIcon.builder()
                .name("혜성 꼬리")
                .description("추천수 50 달성 시 구매 가능.")
                .iconUrl("/icons/aurora_phoenix.svg")
                .price(200)
                .grade(StellaIconGrade.RARE)
                .type(StellaIcon.IconType.ANIMATED)
                .available(true)
                .animationClass("animate-pulse")
                .build()
        );

        stellaIconRepository.saveAll(icons);
        log.info("스텔라 아이콘 {} 개가 성공적으로 생성되었습니다.", icons.size());
    }
}