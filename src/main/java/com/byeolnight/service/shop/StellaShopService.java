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

import java.util.List;

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
     * 기본 아이콘 초기화
     */
    @Transactional
    public void initializeDefaultIcons() {
        log.info("스텔라 아이콘 강제 초기화 시작");
        
        // 기존 데이터 삭제
        stellaIconRepository.deleteAll();
        
        // 기본 아이콘 생성
        List<StellaIcon> defaultIcons = Arrays.asList(
            createIcon("별", "밤하늘의 반짝이는 별", "Star", StellaIconGrade.COMMON, 50),
            createIcon("지구", "우리의 푸른 행성", "Earth", StellaIconGrade.RARE, 150),
            createIcon("달", "지구의 아름다운 위성", "Moon", StellaIconGrade.COMMON, 50),
            createIcon("블랙홀", "시공간을 지배하는 절대적 존재", "BlackHole", StellaIconGrade.LEGENDARY, 500),
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