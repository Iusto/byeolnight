package com.byeolnight.service.shop;

import com.byeolnight.domain.entity.shop.StellaIcon;
import com.byeolnight.domain.entity.shop.StellaIconGrade;
import com.byeolnight.domain.entity.shop.UserIcon;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.shop.StellaIconRepository;
import com.byeolnight.domain.repository.shop.UserIconRepository;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.infrastructure.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StellaShopService {

    private final StellaIconRepository stellaIconRepository;
    private final UserIconRepository userIconRepository;
    private final UserRepository userRepository;
    private final com.byeolnight.service.user.PointService pointService;

    /**
     * 상점 아이콘 목록 조회
     */
    @Transactional(readOnly = true)
    public List<StellaIcon> getShopIcons() {
        List<StellaIcon> icons = stellaIconRepository.findByAvailableTrue();
        log.info("상점 아이콘 조회 결과: {} 개", icons.size());
        return icons;
    }
    
    /**
     * 아이콘 총 개수 조회
     */
    @Transactional(readOnly = true)
    public long getIconCount() {
        return stellaIconRepository.count();
    }

    /**
     * 아이콘 구매
     */
    @Transactional
    public void purchaseIcon(User user, Long iconId) {
        StellaIcon icon = stellaIconRepository.findById(iconId)
                .orElseThrow(() -> new NotFoundException("아이콘을 찾을 수 없습니다."));

        // 구매 가능 여부 확인
        if (!icon.isAvailable()) {
            throw new IllegalArgumentException("구매할 수 없는 아이콘입니다.");
        }

        // 중복 구매 확인
        StellaIcon icon2 = stellaIconRepository.findById(iconId).orElse(null);
        if (icon2 != null && userIconRepository.existsByUserAndStellaIcon(user, icon2)) {
            throw new IllegalArgumentException("이미 보유한 아이콘입니다.");
        }

        // 포인트 확인 및 차감 (PointService에서 처리)
        pointService.recordIconPurchase(user, icon.getId(), icon.getName(), icon.getPrice());

        // 보관함에 추가
        UserIcon userIcon = UserIcon.of(user, icon, icon.getPrice());
        userIconRepository.save(userIcon);
    }

    /**
     * 사용자 보관함 조회
     */
    @Transactional(readOnly = true)
    public List<UserIcon> getUserIcons(User user) {
        return userIconRepository.findByUserWithStellaIconOrderByCreatedAtDesc(user);
    }

    /**
     * 아이콘 장착
     */
    @Transactional
    public void equipIcon(User user, Long iconId) {
        // 기존 장착 아이콘 해제
        userIconRepository.findByUserAndEquippedTrue(user)
                .ifPresent(UserIcon::unequip);

        // 새 아이콘 장착
        StellaIcon iconToEquip = stellaIconRepository.findById(iconId)
                .orElseThrow(() -> new NotFoundException("아이콘을 찾을 수 없습니다."));
        UserIcon userIcon = userIconRepository.findByUserAndStellaIcon(user, iconToEquip)
                .orElseThrow(() -> new NotFoundException("보유하지 않은 아이콘입니다."));
        
        userIcon.equip();
        user.equipIcon(iconId, iconToEquip.getIconUrl()); // 아이콘 이름도 함께 저장
        userRepository.save(user);
    }

    /**
     * 아이콘 해제
     */
    @Transactional
    public void unequipIcon(User user) {
        userIconRepository.findByUserAndEquippedTrue(user)
                .ifPresent(UserIcon::unequip);
        
        user.unequipIcon();
        userRepository.save(user);
    }

    /**
     * 장착된 아이콘 정보 조회
     */
    @Transactional(readOnly = true)
    public com.byeolnight.dto.shop.EquippedIconDto getEquippedIconInfo(Long iconId) {
        StellaIcon icon = stellaIconRepository.findById(iconId)
                .orElse(null);
        
        if (icon == null) {
            return null;
        }
        
        return com.byeolnight.dto.shop.EquippedIconDto.builder()
                .iconId(icon.getId())
                .iconName(icon.getIconUrl())
                .iconUrl(icon.getIconUrl())
                .build();
    }

    /**
     * 기본 아이콘 초기화 - 전체 44개 아이콘
     */
    @Transactional
    public void initializeDefaultIcons() {
        log.info("스텔라 아이콘 강제 초기화 시작");
        
        // 기존 데이터 삭제
        stellaIconRepository.deleteAll();
        
        // 전체 아이콘 생성
        List<StellaIcon> defaultIcons = Arrays.asList(
            // COMMON 등급 (100 스텔라) - 9개
            createIcon("수성", "태양에 가장 가까운 행성", "Mercury", StellaIconGrade.COMMON, 100),
            createIcon("금성", "아름다운 새벽별", "Venus", StellaIconGrade.COMMON, 100),
            createIcon("화성", "붉은 행성의 신비", "Mars", StellaIconGrade.COMMON, 100),
            createIcon("별", "밤하늘의 반짝이는 별", "Star", StellaIconGrade.COMMON, 100),
            createIcon("태양", "생명을 주는 따뜻한 별", "Sun", StellaIconGrade.COMMON, 100),
            createIcon("달", "지구의 아름다운 위성", "Moon", StellaIconGrade.COMMON, 100),
            createIcon("혜성", "천년에 한 번 나타나는 신비", "Comet", StellaIconGrade.COMMON, 100),
            createIcon("소행성", "작은 우주 암석", "Asteroid", StellaIconGrade.COMMON, 100),
            createIcon("로켓", "우주를 향한 꿈과 모험", "Rocket", StellaIconGrade.COMMON, 100),
            
            // RARE 등급 (200 스텔라) - 11개
            createIcon("지구", "우리의 푸른 행성", "Earth", StellaIconGrade.RARE, 200),
            createIcon("목성", "거대한 가스 행성", "Jupiter", StellaIconGrade.RARE, 200),
            createIcon("천왕성", "옆으로 누운 신비한 행성", "Uranus", StellaIconGrade.RARE, 200),
            createIcon("해왕성", "푸른 바다의 행성", "Neptune", StellaIconGrade.RARE, 200),
            createIcon("토성", "우아한 고리를 가진 행성", "Saturn", StellaIconGrade.RARE, 200),
            createIcon("오로라 성운", "극지방의 신비한 빛", "AuroraNebula", StellaIconGrade.RARE, 200),
            createIcon("유성우", "소원을 들어주는 별똥별", "MeteorShower", StellaIconGrade.RARE, 200),
            createIcon("UFO", "미확인 비행 물체", "UFO", StellaIconGrade.RARE, 200),
            createIcon("나선 은하", "아름다운 나선형 은하", "GalaxySpiral", StellaIconGrade.RARE, 200),
            createIcon("별자리", "밤하늘의 아름다운 별 그림", "Constellation", StellaIconGrade.RARE, 200),
            createIcon("우주 정거장", "인류의 우주 진출 기지", "SpaceStation", StellaIconGrade.RARE, 200),
            
            // EPIC 등급 (350 스텔라) - 15개
            createIcon("펄사", "규칙적으로 신호를 보내는 별", "Pulsar", StellaIconGrade.EPIC, 350),
            createIcon("퀘이사", "우주에서 가장 밝은 천체", "Quasar", StellaIconGrade.EPIC, 350),
            createIcon("적색거성", "거대하게 팽창한 늙은 별", "RedGiant", StellaIconGrade.EPIC, 350),
            createIcon("백색왜성", "별의 마지막 잔해", "WhiteDwarf", StellaIconGrade.EPIC, 350),
            createIcon("중성자별", "극도로 압축된 별의 잔해", "NeutronStar", StellaIconGrade.EPIC, 350),
            createIcon("마그네타", "강력한 자기장을 가진 별", "StellarMagnetar", StellaIconGrade.EPIC, 350),
            createIcon("광년", "빛이 1년간 이동하는 거리", "StellarLightYear", StellaIconGrade.EPIC, 350),
            createIcon("안드로메다", "우리 은하의 이웃 은하", "StellarAndromeda", StellaIconGrade.EPIC, 350),
            createIcon("오리온", "겨울 밤하늘의 대표 별자리", "StellarOrion", StellaIconGrade.EPIC, 350),
            createIcon("태양계", "우리 태양계 전체", "StellarSolarSystem", StellaIconGrade.EPIC, 350),
            createIcon("암흑물질", "우주의 숨겨진 물질", "StellarDarkMatter", StellaIconGrade.EPIC, 350),
            createIcon("은하수", "우리 은하계의 전체 모습", "StellarMilkyWay", StellaIconGrade.EPIC, 350),
            createIcon("코스모스", "우주 전체를 아우르는 존재", "StellarCosmos", StellaIconGrade.EPIC, 350),
            createIcon("중력파", "시공간의 파동", "GravitationalWave", StellaIconGrade.EPIC, 350),
            createIcon("타임루프", "시간의 순환과 무한반복", "TimeLoop", StellaIconGrade.EPIC, 350),
            
            // LEGENDARY 등급 (500 스텔라) - 8개
            createIcon("블랙홀", "시공간을 지배하는 절대적 존재", "BlackHole", StellaIconGrade.LEGENDARY, 500),
            createIcon("초신성", "별의 장엄한 마지막 순간", "Supernova", StellaIconGrade.LEGENDARY, 500),
            createIcon("다크에너지", "우주 가속 팽창의 신비로운 에너지", "DarkEnergy", StellaIconGrade.LEGENDARY, 500),
            createIcon("양자터널", "양자역학의 터널링 현상", "QuantumTunnel", StellaIconGrade.LEGENDARY, 500),
            createIcon("다중우주", "무수히 많은 평행우주들", "Multiverse", StellaIconGrade.LEGENDARY, 500),
            createIcon("스트링이론", "진동하는 끈들로 이루어진 우주 구조", "StringTheory", StellaIconGrade.LEGENDARY, 500),
            createIcon("무한우주", "끝없이 확장하는 우주 자체", "StellarInfiniteUniverse", StellaIconGrade.LEGENDARY, 500),
            createIcon("웜홀", "시공간을 연결하는 통로", "Wormhole", StellaIconGrade.LEGENDARY, 500),
            
            // MYTHIC 등급 (1000 스텔라) - 1개
            createIcon("빅뱅", "우주 탄생의 순간", "BigBang", StellaIconGrade.MYTHIC, 1000)
        );
        
        stellaIconRepository.saveAll(defaultIcons);
        log.info("스텔라 아이콘 초기화 완료: {} 개 아이콘 생성", defaultIcons.size());
    }
    
    private StellaIcon createIcon(String name, String description, String iconUrl, StellaIconGrade grade, int price) {
        return StellaIcon.builder()
                .name(name)
                .description(description)
                .iconUrl(iconUrl)
                .grade(grade)
                .price(price)
                .type(StellaIcon.IconType.STATIC)
                .available(true)
                .build();
    }
}