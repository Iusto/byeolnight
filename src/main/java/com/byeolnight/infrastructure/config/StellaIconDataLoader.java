package com.byeolnight.infrastructure.config;

import com.byeolnight.domain.entity.shop.StellaIcon;
import com.byeolnight.domain.entity.shop.StellaIconGrade;
import com.byeolnight.domain.repository.shop.StellaIconRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class StellaIconDataLoader implements CommandLineRunner {

    private final StellaIconRepository stellaIconRepository;
    private final StellaIconConfig iconConfig;

    @Override
    @Transactional
    public void run(String... args) {
        long iconCount = stellaIconRepository.count();
        log.info("스텔라 아이콘 데이터 현황: {} 개", iconCount);
        
        // 이미 데이터가 있으면 로드하지 않음
        if (iconCount > 0) {
            log.info("스텔라 아이콘 데이터가 이미 존재합니다. 로드를 건너뜁니다.");
            
            // 디버깅: 실제 데이터 내용 확인
            stellaIconRepository.findAll().forEach(icon -> 
                log.info("아이콘: {} - {} ({})", icon.getName(), icon.getDescription(), icon.getGrade())
            );
            return;
        }
        
        log.info("프론트엔드 아이콘 컴포넌트 스캔 중...");
        loadIconsFromFrontend();
        log.info("스텔라 아이콘 자동 로드 완료! 총 {} 개 아이콘 로드됨", stellaIconRepository.count());
    }

    private void loadIconsFromFrontend() {
        try {
            List<StellaIcon> icons = scanIconComponents();
            stellaIconRepository.saveAll(icons);
        } catch (Exception e) {
            log.error("아이콘 컴포넌트 스캔 실패: {}", e.getMessage());
            // 실패 시 기본 데이터 로드
            loadDefaultIcons();
        }
    }
    
    private List<StellaIcon> scanIconComponents() {
        List<StellaIcon> icons = new ArrayList<>();
        
        Path iconsPath = Paths.get(iconConfig.getFolderPath());
        
        if (!Files.exists(iconsPath)) {
            log.warn("아이콘 폴더를 찾을 수 없습니다: {}", iconConfig.getFolderPath());
            return getDefaultIcons();
        }
        
        try (Stream<Path> files = Files.walk(iconsPath)) {
            files.filter(path -> path.toString().endsWith(iconConfig.getFileExtension()))
                 .filter(path -> !iconConfig.getExcludeFiles().contains(path.getFileName().toString()))
                 .forEach(path -> {
                     String fileName = path.getFileName().toString().replace(iconConfig.getFileExtension(), "");
                     StellaIcon icon = createIconFromFileName(fileName);
                     if (icon != null) {
                         icons.add(icon);
                     }
                 });
        } catch (IOException e) {
            log.error("아이콘 파일 스캔 오류: {}", e.getMessage());
        }
        
        return icons.isEmpty() ? getDefaultIcons() : icons;
    }
    
    private StellaIcon createIconFromFileName(String fileName) {
        StellaIconGrade grade = iconConfig.getGradeByIconName(fileName);
        int price = iconConfig.getPriceByGrade(grade);
        String koreanName = iconConfig.getKoreanName(fileName);
        String description = iconConfig.getDescription(fileName);
        
        return createIcon(koreanName, description, fileName, grade, price);
    }
    

    
    private void loadDefaultIcons() {
        List<StellaIcon> defaultIcons = getDefaultIcons();
        stellaIconRepository.saveAll(defaultIcons);
    }
    
    private List<StellaIcon> getDefaultIcons() {
        return Arrays.asList(
            createIcon("별", "밤하늘의 반짝이는 별", "Star", StellaIconGrade.COMMON, 50),
            createIcon("지구", "우리의 푸른 행성", "Earth", StellaIconGrade.RARE, 150),
            createIcon("달", "지구의 아름다운 위성", "Moon", StellaIconGrade.COMMON, 50),
            createIcon("블랙홀", "시공간을 지배하는 절대적 존재", "BlackHole", StellaIconGrade.LEGENDARY, 500),
            createIcon("빅뱅", "우주 탄생의 순간", "BigBang", StellaIconGrade.MYTHIC, 1000)
        );
    }

    private StellaIcon createIcon(String name, String description, String iconUrl, StellaIconGrade grade, int price) {
        return StellaIcon.builder()
                .name(name)
                .description(description)
                .iconUrl(iconUrl)
                .grade(grade)
                .price(price)
                .type(price >= 0 ? StellaIcon.IconType.STATIC : StellaIcon.IconType.SPECIAL)
                .available(price >= 0)
                .build();
    }
}