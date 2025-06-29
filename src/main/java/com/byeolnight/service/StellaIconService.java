package com.byeolnight.service;

import com.byeolnight.domain.entity.shop.StellaIcon;
import com.byeolnight.domain.entity.shop.UserIcon;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.shop.StellaIconRepository;
import com.byeolnight.domain.repository.shop.UserIconRepository;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.dto.shop.StellaIconDto;
import com.byeolnight.dto.shop.UserIconDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StellaIconService {

    private final StellaIconRepository stellaIconRepository;
    private final UserIconRepository userIconRepository;
    private final UserRepository userRepository;

    /**
     * 상점 아이콘 목록 조회 (사용자 보유 여부 포함)
     */
    public List<StellaIconDto> getShopIcons(String email) {
        List<StellaIcon> availableIcons = stellaIconRepository.findByAvailableTrue();
        
        final Set<Long> ownedIconIds;
        if (email != null) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            ownedIconIds = userIconRepository.findByUser(user)
                    .stream()
                    .map(userIcon -> userIcon.getStellaIcon().getId())
                    .collect(Collectors.toSet());
        } else {
            ownedIconIds = Set.of();
        }

        return availableIcons.stream()
                .map(icon -> StellaIconDto.from(icon, ownedIconIds.contains(icon.getId())))
                .collect(Collectors.toList());
    }

    /**
     * 아이콘 구매
     */
    @Transactional
    public void purchaseIcon(String email, Long iconId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        StellaIcon icon = stellaIconRepository.findById(iconId)
                .orElseThrow(() -> new IllegalArgumentException("아이콘을 찾을 수 없습니다."));

        if (!icon.isAvailable()) {
            throw new IllegalArgumentException("구매할 수 없는 아이콘입니다.");
        }

        // 이미 보유 중인지 확인
        boolean alreadyOwned = userIconRepository.existsByUserAndStellaIcon(user, icon);
        if (alreadyOwned) {
            throw new IllegalArgumentException("이미 보유 중인 아이콘입니다.");
        }

        // 포인트 차감
        user.decreasePoints(icon.getPrice());
        
        // 아이콘 구매 기록 생성
        UserIcon userIcon = UserIcon.of(user, icon, icon.getPrice());
        userIconRepository.save(userIcon);
        
        userRepository.save(user);
    }

    /**
     * 내 아이콘 보관함 조회
     */
    public List<UserIconDto> getMyIcons(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<UserIcon> userIcons = userIconRepository.findByUserOrderByCreatedAtDesc(user);
        
        return userIcons.stream()
                .map(UserIconDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 아이콘 장착
     */
    @Transactional
    public void equipIcon(String email, Long iconId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        StellaIcon icon = stellaIconRepository.findById(iconId)
                .orElseThrow(() -> new IllegalArgumentException("아이콘을 찾을 수 없습니다."));

        // 보유 중인 아이콘인지 확인
        UserIcon userIcon = userIconRepository.findByUserAndStellaIcon(user, icon)
                .orElseThrow(() -> new IllegalArgumentException("보유하지 않은 아이콘입니다."));

        // 기존 장착 아이콘 해제
        userIconRepository.findByUserAndEquippedTrue(user)
                .ifPresent(equipped -> equipped.unequip());

        // 새 아이콘 장착
        userIcon.equip();
        user.equipIcon(iconId);
        
        userRepository.save(user);
    }

    /**
     * 아이콘 해제
     */
    @Transactional
    public void unequipIcon(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 장착 중인 아이콘 해제
        userIconRepository.findByUserAndEquippedTrue(user)
                .ifPresent(equipped -> equipped.unequip());

        user.unequipIcon();
        userRepository.save(user);
    }
}