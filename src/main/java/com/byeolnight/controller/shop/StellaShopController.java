package com.byeolnight.controller.shop;

import com.byeolnight.entity.shop.StellaIcon;
import com.byeolnight.entity.shop.UserIcon;
import com.byeolnight.entity.user.User;
import com.byeolnight.dto.shop.StellaIconDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.shop.StellaShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "🛍️ 스텔라 상점 API", description = "스텔라 아이콘 구매 및 장착 API")
public class StellaShopController {

    private final StellaShopService stellaShopService;

    @Operation(summary = "스텔라 상점 아이콘 목록 조회 (공개)")
    @GetMapping("/api/public/shop/icons")
    public ResponseEntity<CommonResponse<List<StellaIconDto>>> getShopIcons(
            @AuthenticationPrincipal User user) {
        
        log.info("아이콘 목록 요청 - 사용자: {}", user != null ? user.getNickname() : "null");
        
        try {
            List<StellaIcon> shopIcons = stellaShopService.getShopIcons();
            log.info("상점 아이콘 조회 - 사용자: {}, 아이콘 수: {}", 
                    user != null ? user.getNickname() : "anonymous", shopIcons.size());
            
            // 빈 리스트일 때 기본 데이터 초기화
            if (shopIcons.isEmpty()) {
                log.warn("아이콘 데이터가 비어있음. 기본 데이터 초기화 시도");
                stellaShopService.initializeDefaultIcons();
                shopIcons = stellaShopService.getShopIcons();
                log.info("기본 데이터 초기화 후 아이콘 수: {}", shopIcons.size());
            }
            
            Set<Long> ownedIconIds = user != null ? 
                stellaShopService.getUserIcons(user).stream()
                    .map(userIcon -> userIcon.getStellaIcon().getId())
                    .collect(Collectors.toSet()) : Set.of();
            
            List<StellaIconDto> iconDtos = shopIcons.stream()
                    .map(icon -> StellaIconDto.from(icon, ownedIconIds.contains(icon.getId())))
                    .toList();
            
            log.info("아이콘 DTO 변환 완료 - DTO 수: {}", iconDtos.size());
            return ResponseEntity.ok(CommonResponse.success(iconDtos));
        } catch (Exception e) {
            log.error("아이콘 목록 조회 중 오류 발생", e);
            return ResponseEntity.ok(CommonResponse.error("아이콘 목록 조회 실패: " + e.getMessage()));
        }
    }

    @Operation(summary = "스텔라 아이콘 구매 (로그인 필요)")
    @PostMapping("/api/member/shop/purchase/{iconId}")
    public ResponseEntity<CommonResponse<String>> purchaseIcon(
            @PathVariable Long iconId,
            @AuthenticationPrincipal User user) {
        
        log.info("아이콘 구매 요청 - 사용자: {}, 아이콘ID: {}", user.getNickname(), iconId);
        
        try {
            stellaShopService.purchaseIcon(user, iconId);
            return ResponseEntity.ok(CommonResponse.success("아이콘을 성공적으로 구매했습니다."));
        } catch (Exception e) {
            return ResponseEntity.ok(CommonResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "내 보관함 조회 (로그인 필요)")
    @GetMapping("/api/member/shop/my-icons-legacy")
    public ResponseEntity<CommonResponse<List<com.byeolnight.dto.shop.UserIconDto>>> getMyIcons(
            @AuthenticationPrincipal User user) {
        
        log.info("보유 아이콘 요청 - 사용자: {}", user.getNickname());
        
        try {
            List<UserIcon> userIcons = stellaShopService.getUserIcons(user);
            List<com.byeolnight.dto.shop.UserIconDto> userIconDtos = userIcons.stream()
                    .map(com.byeolnight.dto.shop.UserIconDto::from)
                    .toList();
            return ResponseEntity.ok(CommonResponse.success(userIconDtos));
        } catch (Exception e) {
            return ResponseEntity.ok(CommonResponse.error("보유 아이콘 조회 실패: " + e.getMessage()));
        }
    }

    @Operation(summary = "내 보관함 조회")
    @GetMapping("/api/member/shop/my-icons")
    public ResponseEntity<CommonResponse<List<com.byeolnight.dto.shop.UserIconDto>>> getMyIconsForMember(
            @AuthenticationPrincipal User user) {
        
        try {
            List<UserIcon> userIcons = stellaShopService.getUserIcons(user);
            List<com.byeolnight.dto.shop.UserIconDto> userIconDtos = userIcons.stream()
                    .map(com.byeolnight.dto.shop.UserIconDto::from)
                    .toList();
            return ResponseEntity.ok(CommonResponse.success(userIconDtos));
        } catch (Exception e) {
            return ResponseEntity.ok(CommonResponse.error("보유 아이콘 조회 실패: " + e.getMessage()));
        }
    }

    @Operation(summary = "아이콘 장착")
    @PostMapping("/api/member/shop/icons/{iconId}/equip")
    public ResponseEntity<CommonResponse<String>> equipIconForMember(
            @PathVariable Long iconId,
            @AuthenticationPrincipal User user) {
        
        log.info("아이콘 장착 요청 - 사용자: {}, 아이콘ID: {}", user.getNickname(), iconId);
        
        try {
            stellaShopService.equipIcon(user, iconId);
            log.info("아이콘 장착 성공 - 사용자: {}, 아이콘ID: {}", user.getNickname(), iconId);
            return ResponseEntity.ok(CommonResponse.success("아이콘을 장착했습니다."));
        } catch (Exception e) {
            log.error("아이콘 장착 실패 - 사용자: {}, 아이콘ID: {}, 오류: {}", user.getNickname(), iconId, e.getMessage(), e);
            return ResponseEntity.ok(CommonResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "아이콘 해제")
    @PostMapping("/api/member/shop/icons/{iconId}/unequip")
    public ResponseEntity<CommonResponse<String>> unequipIconForMember(
            @PathVariable Long iconId,
            @AuthenticationPrincipal User user) {
        
        log.info("아이콘 해제 요청 - 사용자: {}, 아이콘ID: {}", user.getNickname(), iconId);
        
        try {
            stellaShopService.unequipIcon(user);
            log.info("아이콘 해제 성공 - 사용자: {}", user.getNickname());
            return ResponseEntity.ok(CommonResponse.success("아이콘을 해제했습니다."));
        } catch (Exception e) {
            log.error("아이콘 해제 실패 - 사용자: {}, 오류: {}", user.getNickname(), e.getMessage(), e);
            return ResponseEntity.ok(CommonResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "스텔라 아이콘 초기화 (개발용)")
    @PostMapping("/api/shop/init-icons")
    public ResponseEntity<CommonResponse<String>> initIcons() {
        try {
            stellaShopService.initializeDefaultIcons();
            return ResponseEntity.ok(CommonResponse.success("스텔라 아이콘이 초기화되었습니다."));
        } catch (Exception e) {
            log.error("아이콘 초기화 실패", e);
            return ResponseEntity.ok(CommonResponse.error("초기화 실패: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "디버깅: 아이콘 데이터 확인")
    @GetMapping("/api/shop/debug")
    public ResponseEntity<CommonResponse<String>> debugIcons() {
        try {
            long count = stellaShopService.getIconCount();
            List<StellaIcon> icons = stellaShopService.getShopIcons();
            StringBuilder sb = new StringBuilder();
            sb.append("Total icons: ").append(count).append("\n");
            icons.forEach(icon -> sb.append(icon.getName()).append(" - ").append(icon.getGrade()).append("\n"));
            return ResponseEntity.ok(CommonResponse.success(sb.toString()));
        } catch (Exception e) {
            log.error("디버깅 오류", e);
            return ResponseEntity.ok(CommonResponse.error("디버깅 실패: " + e.getMessage()));
        }
    }
}