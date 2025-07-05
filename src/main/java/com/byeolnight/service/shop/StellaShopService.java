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
     * 기본 아이콘 초기화 (StellaIconDataLoader에서 처리)
     */
    @Transactional
    public void initializeDefaultIcons() {
        // StellaIconDataLoader에서 이미 처리하므로 별도 로직 불필요
        // 필요시 강제 재로드를 위해 기존 데이터 삭제 후 재시작 필요
        throw new UnsupportedOperationException("아이콘 초기화는 애플리케이션 재시작을 통해 수행됩니다.");
    }
}