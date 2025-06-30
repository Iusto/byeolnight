package com.byeolnight.service.shop;

import com.byeolnight.domain.entity.shop.StellaIcon;
import com.byeolnight.domain.entity.shop.UserIcon;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.shop.StellaIconRepository;
import com.byeolnight.domain.repository.shop.UserIconRepository;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.infrastructure.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StellaShopService {

    private final StellaIconRepository stellaIconRepository;
    private final UserIconRepository userIconRepository;
    private final UserRepository userRepository;

    /**
     * 상점 아이콘 목록 조회
     */
    @Transactional(readOnly = true)
    public List<StellaIcon> getShopIcons() {
        return stellaIconRepository.findByAvailableTrue();
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

        // 포인트 확인 및 차감
        user.decreasePoints(icon.getPrice());
        userRepository.save(user);

        // 보관함에 추가
        UserIcon userIcon = UserIcon.of(user, icon, icon.getPrice());
        userIconRepository.save(userIcon);
    }

    /**
     * 사용자 보관함 조회
     */
    @Transactional(readOnly = true)
    public List<UserIcon> getUserIcons(User user) {
        return userIconRepository.findByUserOrderByCreatedAtDesc(user);
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
        user.equipIcon(iconId);
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
     * 기본 아이콘 초기화
     */
    @Transactional
    public void initializeDefaultIcons() {
        if (stellaIconRepository.count() > 0) {
            return; // 이미 데이터가 있으면 스킵
        }

        List<StellaIcon> icons = List.of(
            StellaIcon.builder()
                .name("반짝이는 별")
                .description("가장 기본적인 별 아이콘입니다.")
                .iconUrl("/icons/basic_star.svg")
                .price(0)
                .grade(StellaIcon.IconGrade.COMMON)
                .type(StellaIcon.IconType.STATIC)
                .available(true)
                .animationClass("")
                .build(),
            
            StellaIcon.builder()
                .name("반짝이는 유성")
                .description("처음 포인트를 획득하면 구매할 수 있습니다.")
                .iconUrl("/icons/shiny_meteor.svg")
                .price(50)
                .grade(StellaIcon.IconGrade.COMMON)
                .type(StellaIcon.IconType.ANIMATED)
                .available(true)
                .animationClass("animate-pulse")
                .build(),

            StellaIcon.builder()
                .name("황혼의 행성")
                .description("아름다운 황혼 빛의 행성")
                .iconUrl("/icons/twilight_planet.svg")
                .price(100)
                .grade(StellaIcon.IconGrade.RARE)
                .type(StellaIcon.IconType.ANIMATED)
                .available(true)
                .animationClass("animate-pulse")
                .build(),

            StellaIcon.builder()
                .name("자정의 달")
                .description("신비로운 자정의 달")
                .iconUrl("/icons/midnight_moon.svg")
                .price(150)
                .grade(StellaIcon.IconGrade.RARE)
                .type(StellaIcon.IconType.ANIMATED)
                .available(true)
                .animationClass("animate-bounce")
                .build(),

            StellaIcon.builder()
                .name("오로라 피닉스")
                .description("전설의 불사조 아이콘")
                .iconUrl("/icons/aurora_phoenix.svg")
                .price(300)
                .grade(StellaIcon.IconGrade.LEGENDARY)
                .type(StellaIcon.IconType.SPECIAL)
                .available(true)
                .animationClass("animate-pulse")
                .build()
        );

        stellaIconRepository.saveAll(icons);
    }
}