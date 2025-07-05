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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class StellaIconDataLoader implements CommandLineRunner {

    private final StellaIconRepository stellaIconRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // 이미 데이터가 있으면 로드하지 않음
        if (stellaIconRepository.count() > 0) {
            log.info("스텔라 아이콘 데이터가 이미 존재합니다. 로드를 건너뜁니다.");
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
        
        // 프론트엔드 아이콘 폴더 경로
        String iconFolderPath = "byeolnight-frontend/src/components/icons";
        Path iconsPath = Paths.get(iconFolderPath);
        
        if (!Files.exists(iconsPath)) {
            log.warn("아이콘 폴더를 찾을 수 없습니다: {}", iconFolderPath);
            return getDefaultIcons();
        }
        
        try (Stream<Path> files = Files.walk(iconsPath)) {
            files.filter(path -> path.toString().endsWith(".tsx"))
                 .filter(path -> !path.getFileName().toString().equals("index.tsx"))
                 .forEach(path -> {
                     String fileName = path.getFileName().toString().replace(".tsx", "");
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
        // 파일명에 따른 등급 및 가격 결정
        StellaIconGrade grade;
        int price;
        String koreanName;
        String description;
        
        // COMMON 등급 - 기본 천체들 (100 스텔라)
        if (Arrays.asList("Mercury", "Venus", "Mars", "Star", "Sun", "Moon", "Comet", "Asteroid", "Rocket").contains(fileName)) {
            grade = StellaIconGrade.COMMON;
            price = 100;
        }
        // RARE 등급 - 특별한 우주 현상들 (200 스텔라)
        else if (Arrays.asList("Earth", "Jupiter", "Uranus", "Neptune", "Saturn", "AuroraNebula", "MeteorShower", "UFO", "GalaxySpiral", "Constellation", "SpaceStation", "CosmicVortex").contains(fileName)) {
            grade = StellaIconGrade.RARE;
            price = 200;
        }
        // EPIC 등급 - 환상적인 우주의 신비 (350 스텔라)
        else if (Arrays.asList("Pulsar", "Quasar", "RedGiant", "WhiteDwarf", "NeutronStar", "StellarMagnetar", "StellarLightYear", "StellarAndromeda", "StellarOrion", "StellarSolarSystem", "StellarDarkMatter", "StellarMilkyWay", "StellarCosmos", "GravitationalWave", "TimeLoop").contains(fileName)) {
            grade = StellaIconGrade.EPIC;
            price = 350;
        }
        // LEGENDARY 등급 - 전설적인 우주의 보물 (500 스텔라)
        else if (Arrays.asList("BlackHole", "Supernova", "DarkEnergy", "QuantumTunnel", "Multiverse", "StringTheory", "StellarInfiniteUniverse").contains(fileName)) {
            grade = StellaIconGrade.LEGENDARY;
            price = 500;
        }
        // MYTHIC 등급 - 신화적 우주 존재들 (1000 스텔라)
        else if (Arrays.asList("BigBang").contains(fileName)) {
            grade = StellaIconGrade.MYTHIC;
            price = 1000;
        }
        else {
            // 기본값
            grade = StellaIconGrade.COMMON;
            price = 100;
        }
        
        // 한국어 이름 매핑
        koreanName = getKoreanName(fileName);
        description = getDescription(fileName);
        
        return createIcon(koreanName, description, fileName, grade, price);
    }
    
    private String getKoreanName(String fileName) {
        Map<String, String> nameMap = new HashMap<>();
        // COMMON
        nameMap.put("Mercury", "수성");
        nameMap.put("Venus", "금성");
        nameMap.put("Mars", "화성");
        nameMap.put("Star", "별");
        nameMap.put("Sun", "태양");
        nameMap.put("Moon", "달");
        nameMap.put("Comet", "혜성");
        nameMap.put("Asteroid", "소행성");
        nameMap.put("Rocket", "로켓");
        nameMap.put("Saturn", "토성");
        
        // RARE
        nameMap.put("Earth", "지구");
        nameMap.put("Jupiter", "목성");
        nameMap.put("Uranus", "천왕성");
        nameMap.put("Neptune", "해왕성");
        nameMap.put("AuroraNebula", "오로라 성운");
        nameMap.put("MeteorShower", "유성우");
        nameMap.put("UFO", "UFO");
        nameMap.put("GalaxySpiral", "나선 은하");
        nameMap.put("Constellation", "별자리");
        nameMap.put("SpaceStation", "우주 정거장");
        nameMap.put("CosmicVortex", "우주 소용돌이");
        
        // EPIC
        nameMap.put("Pulsar", "펄사");
        nameMap.put("Quasar", "퀘이사");
        nameMap.put("RedGiant", "적색거성");
        nameMap.put("WhiteDwarf", "백색왜성");
        nameMap.put("NeutronStar", "중성자별");
        nameMap.put("StellarMagnetar", "마그네타");
        nameMap.put("StellarLightYear", "광년");
        nameMap.put("StellarAndromeda", "안드로메다");
        nameMap.put("StellarOrion", "오리온");
        nameMap.put("StellarSolarSystem", "태양계");
        nameMap.put("StellarDarkMatter", "암흑물질");
        nameMap.put("StellarMilkyWay", "은하수");
        nameMap.put("StellarCosmos", "코스모스");
        nameMap.put("GravitationalWave", "중력파");
        nameMap.put("TimeLoop", "타임루프");
        
        // LEGENDARY
        nameMap.put("BlackHole", "블랙홀");
        nameMap.put("Supernova", "초신성");
        nameMap.put("DarkEnergy", "다크에너지");
        nameMap.put("QuantumTunnel", "양자터널");
        nameMap.put("Multiverse", "다중우주");
        nameMap.put("StringTheory", "스트링이론");
        nameMap.put("StellarInfiniteUniverse", "무한우주");
        
        // MYTHIC
        nameMap.put("BigBang", "빅뱅");
        
        return nameMap.getOrDefault(fileName, fileName);
    }
    
    private String getDescription(String fileName) {
        Map<String, String> descMap = new HashMap<>();
        // COMMON
        descMap.put("Mercury", "태양에 가장 가까운 행성");
        descMap.put("Venus", "아름다운 새벽별");
        descMap.put("Mars", "붉은 행성의 신비");
        descMap.put("Star", "밤하늘의 반짝이는 별");
        descMap.put("Sun", "생명을 주는 따뜻한 별");
        descMap.put("Moon", "지구의 아름다운 위성");
        descMap.put("Comet", "천년에 한 번 나타나는 신비");
        descMap.put("Asteroid", "작은 우주 암석");
        descMap.put("Rocket", "우주를 향한 꿈과 모험");
        descMap.put("Saturn", "우아한 고리를 가진 행성");
        
        // RARE
        descMap.put("Earth", "우리의 푸른 행성");
        descMap.put("Jupiter", "거대한 가스 행성");
        descMap.put("Uranus", "옆으로 누운 신비한 행성");
        descMap.put("Neptune", "푸른 바다의 행성");
        descMap.put("AuroraNebula", "극지방의 신비한 빛");
        descMap.put("MeteorShower", "소원을 들어주는 별똥별");
        descMap.put("UFO", "미확인 비행 물체");
        descMap.put("GalaxySpiral", "아름다운 나선형 은하");
        descMap.put("Constellation", "밤하늘의 아름다운 별 그림");
        descMap.put("SpaceStation", "인류의 우주 진출 기지");
        descMap.put("CosmicVortex", "시공간을 빨아들이는 우주 소용돌이");
        
        // EPIC
        descMap.put("Pulsar", "규칙적으로 신호를 보내는 별");
        descMap.put("Quasar", "우주에서 가장 밝은 천체");
        descMap.put("RedGiant", "거대하게 팽창한 늙은 별");
        descMap.put("WhiteDwarf", "다이아몬드처럼 빛나는 별의 영원한 유산, 우주의 보석함에서 가장 순수하게 빛나는 천체의 마지막 선물");
        descMap.put("NeutronStar", "초당 수백 번 회전하며 강렬한 에너지 빔을 방출하는 우주의 등대, 물질의 극한을 보여주는 신비로운 천체");
        descMap.put("StellarMagnetar", "강력한 자기장을 가진 별");
        descMap.put("StellarLightYear", "빛이 1년간 이동하는 거리");
        descMap.put("StellarAndromeda", "우리 은하의 이웃 은하");
        descMap.put("StellarOrion", "겨울 밤하늘의 대표 별자리");
        descMap.put("StellarSolarSystem", "우리 태양계 전체");
        descMap.put("StellarDarkMatter", "보이지 않지만 우주 전체를 지배하는 신비로운 존재, 은하들을 하나로 묶는 보이지 않는 거대한 손길");
        descMap.put("StellarMilkyWay", "우리 은하계의 전체 모습");
        descMap.put("StellarCosmos", "우주 전체를 아우르는 존재");
        descMap.put("GravitationalWave", "시공간의 파동");
        descMap.put("TimeLoop", "시간의 순환과 무한반복");
        
        // LEGENDARY
        descMap.put("BlackHole", "시공간을 지배하는 절대적 존재");
        descMap.put("Supernova", "별의 장엄한 마지막 순간");
        descMap.put("DarkEnergy", "우주 팽창을 가속화하는 신비한 힘");
        descMap.put("QuantumTunnel", "양자역학의 신비를 보여주는 차원의 통로");
        descMap.put("Multiverse", "무한한 가능성이 존재하는 평행우주들");
        descMap.put("StringTheory", "모든 것을 설명하는 우주의 근본 이론");
        descMap.put("StellarInfiniteUniverse", "끝없이 펼쳐진 무한한 우주의 신비");
        
        // MYTHIC
        descMap.put("BigBang", "무(無)에서 시작된 우주 창조의 순간! 상상할 수 없는 에너지가 폭발하며 시간과 공간이 탄생하고, 모든 물질과 에너지가 한 점에서 무한히 확장되어 138억 년의 우주 역사가 시작되는 절대적이고 경이로운 창조의 빅뱅! 가속 팽창의 신비로운 에너지");
        descMap.put("QuantumTunnel", "양자역학의 터널링 현상");
        descMap.put("Multiverse", "무수히 많은 평행우주들");
        descMap.put("StringTheory", "진동하는 끈들로 이루어진 우주 구조");
        descMap.put("StellarInfiniteUniverse", "끝없이 확장하는 우주 자체");
        
        // MYTHIC
        descMap.put("BigBang", "우주 탄생의 순간");
        
        return descMap.getOrDefault(fileName, fileName + " 아이콘");
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