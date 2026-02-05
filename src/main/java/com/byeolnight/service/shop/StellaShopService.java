package com.byeolnight.service.shop;

import com.byeolnight.entity.shop.StellaIcon;
import com.byeolnight.entity.shop.UserIcon;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.shop.StellaIconRepository;
import com.byeolnight.repository.shop.UserIconRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.dto.shop.StellaIconDataDto;
import com.byeolnight.infrastructure.exception.NotFoundException;
import com.byeolnight.service.user.PointService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StellaShopService {

    private final StellaIconRepository stellaIconRepository;
    private final UserIconRepository userIconRepository;
    private final UserRepository userRepository;
    private final PointService pointService;
    private final ObjectMapper objectMapper;

    /**
     * 상점 아이콘 목록 조회
     */
    @Transactional(readOnly = true)
    public List<StellaIcon> getShopIcons() {
        List<StellaIcon> icons = stellaIconRepository.findByAvailableTrueOrderById();
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
     * DB UNIQUE 제약조건(user_id, stella_icon_id)으로 중복 구매 방지
     */
    @Transactional
    public void purchaseIcon(User user, Long iconId) {
        StellaIcon icon = stellaIconRepository.findById(iconId)
                .orElseThrow(() -> new NotFoundException("아이콘을 찾을 수 없습니다."));

        // 구매 가능 여부 확인
        if (!icon.isAvailable()) {
            throw new IllegalArgumentException("구매할 수 없는 아이콘입니다.");
        }

        try {
            // 포인트 확인 및 차감 (PointService에서 처리)
            pointService.recordIconPurchase(user, icon.getId(), icon.getName(), icon.getPrice());

            // 보관함에 추가 (DB UNIQUE 제약조건으로 중복 방지)
            UserIcon userIcon = UserIcon.of(user, icon, icon.getPrice());
            userIconRepository.save(userIcon);

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // DB 제약조건 위반 = 이미 보유한 아이콘
            throw new IllegalArgumentException("이미 보유한 아이콘입니다.");
        }
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
     * 트랜잭션으로 동시성 제어
     */
    @Transactional
    public void equipIcon(User user, Long iconId) {
        // User를 영속 상태로 다시 조회 (detached 상태 문제 해결)
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        // 기존 장착 아이콘 해제
        userIconRepository.findByUserAndEquippedTrue(managedUser)
                .ifPresent(UserIcon::unequip);

        // 새 아이콘 장착
        StellaIcon iconToEquip = stellaIconRepository.findById(iconId)
                .orElseThrow(() -> new NotFoundException("아이콘을 찾을 수 없습니다."));
        UserIcon userIcon = userIconRepository.findByUserAndStellaIcon(managedUser, iconToEquip)
                .orElseThrow(() -> new NotFoundException("보유하지 않은 아이콘입니다."));

        userIcon.equip();
        managedUser.equipIcon(iconId, iconToEquip.getIconUrl());
    }

    /**
     * 아이콘 해제
     * 트랜잭션으로 동시성 제어
     */
    @Transactional
    public void unequipIcon(User user) {
        // User를 영속 상태로 다시 조회 (detached 상태 문제 해결)
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        userIconRepository.findByUserAndEquippedTrue(managedUser)
                .ifPresent(UserIcon::unequip);

        managedUser.unequipIcon();
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
     * JSON 파일에서 아이콘 데이터를 로드하여 초기화
     */
    @Transactional
    public void initializeDefaultIcons() {
        try {
            log.info("스텔라 아이콘 초기화 시작");
            
            long currentCount = stellaIconRepository.count();
            if (currentCount >= 44) {
                log.info("스텔라 아이콘이 충분합니다. ({}/44) 초기화를 건너뜁니다.", currentCount);
                return;
            }
            
            log.info("스텔라 아이콘이 부족합니다. ({}/44) 기존 데이터를 삭제하고 전체 재초기화합니다.", currentCount);
            stellaIconRepository.deleteAll();
            
            List<StellaIconDataDto> iconDataList = loadIconDataFromJson();
            List<StellaIcon> defaultIcons = iconDataList.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());
            
            stellaIconRepository.saveAll(defaultIcons);
            log.info("스텔라 아이콘 초기화 완료: {} 개 아이콘 생성", defaultIcons.size());
            
        } catch (Exception e) {
            log.error("스텔라 아이콘 초기화 실패", e);
        }
    }
    
    private List<StellaIconDataDto> loadIconDataFromJson() throws IOException {
        ClassPathResource resource = new ClassPathResource("data/stella-icons.json");
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<List<StellaIconDataDto>>() {});
        }
    }
    
    private StellaIcon convertToEntity(StellaIconDataDto dto) {
        return StellaIcon.builder()
            .name(dto.getName())
            .description(dto.getDescription())
            .iconUrl(dto.getIconUrl())
            .grade(dto.getGrade())
            .price(dto.getPrice())
            .available(dto.isAvailable())
            .type(StellaIcon.IconType.STATIC)
            .animationClass(null)
            .build();
    }
}