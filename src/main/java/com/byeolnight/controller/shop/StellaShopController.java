package com.byeolnight.controller.shop;

import com.byeolnight.domain.entity.shop.StellaIcon;
import com.byeolnight.domain.entity.shop.UserIcon;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.dto.shop.StellaIconDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.shop.StellaShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Tag(name = "🛍️ 스텔라 상점 API", description = "스텔라 아이콘 구매 및 장착 API")
public class StellaShopController {

    private final StellaShopService stellaShopService;

    @Operation(summary = "스텔라 상점 아이콘 목록 조회")
    @GetMapping("/api/shop/icons")
    public ResponseEntity<CommonResponse<List<StellaIconDto>>> getShopIcons(
            @AuthenticationPrincipal User user) {
        
        List<StellaIcon> shopIcons = stellaShopService.getShopIcons();
        
        Set<Long> ownedIconIds = user != null ? 
            stellaShopService.getUserIcons(user).stream()
                .map(userIcon -> userIcon.getStellaIcon().getId())
                .collect(Collectors.toSet()) : Set.of();
        
        List<StellaIconDto> iconDtos = shopIcons.stream()
                .map(icon -> StellaIconDto.from(icon, ownedIconIds.contains(icon.getId())))
                .toList();
        
        return ResponseEntity.ok(CommonResponse.success(iconDtos));
    }

    @Operation(summary = "스텔라 아이콘 구매")
    @PostMapping("/api/shop/purchase/{iconId}")
    public ResponseEntity<CommonResponse<String>> purchaseIcon(
            @PathVariable Long iconId,
            @AuthenticationPrincipal User user) {
        
        try {
            stellaShopService.purchaseIcon(user, iconId);
            return ResponseEntity.ok(CommonResponse.success("아이콘을 성공적으로 구매했습니다."));
        } catch (Exception e) {
            return ResponseEntity.ok(CommonResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "내 보관함 조회")
    @GetMapping("/api/shop/my-icons")
    public ResponseEntity<CommonResponse<List<UserIcon>>> getMyIcons(
            @AuthenticationPrincipal User user) {
        
        List<UserIcon> userIcons = stellaShopService.getUserIcons(user);
        return ResponseEntity.ok(CommonResponse.success(userIcons));
    }

    @Operation(summary = "내 보관함 조회 (멤버 전용)")
    @GetMapping("/api/member/shop/my-icons")
    public ResponseEntity<CommonResponse<List<UserIcon>>> getMyIconsForMember(
            @AuthenticationPrincipal User user) {
        
        List<UserIcon> userIcons = stellaShopService.getUserIcons(user);
        return ResponseEntity.ok(CommonResponse.success(userIcons));
    }

    @Operation(summary = "아이콘 장착")
    @PostMapping("/api/shop/equip/{iconId}")
    public ResponseEntity<CommonResponse<String>> equipIcon(
            @PathVariable Long iconId,
            @AuthenticationPrincipal User user) {
        
        try {
            stellaShopService.equipIcon(user, iconId);
            return ResponseEntity.ok(CommonResponse.success("아이콘을 장착했습니다."));
        } catch (Exception e) {
            return ResponseEntity.ok(CommonResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "아이콘 해제")
    @PostMapping("/api/shop/unequip")
    public ResponseEntity<CommonResponse<String>> unequipIcon(
            @AuthenticationPrincipal User user) {
        
        stellaShopService.unequipIcon(user);
        return ResponseEntity.ok(CommonResponse.success("아이콘을 해제했습니다."));
    }

    @Operation(summary = "스텔라 아이콘 초기화 (개발용)")
    @PostMapping("/api/shop/init-icons")
    public ResponseEntity<CommonResponse<String>> initIcons() {
        try {
            stellaShopService.initializeDefaultIcons();
            return ResponseEntity.ok(CommonResponse.success("스텔라 아이콘이 초기화되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.ok(CommonResponse.error("초기화 실패: " + e.getMessage()));
        }
    }
}