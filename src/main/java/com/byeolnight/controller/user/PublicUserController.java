package com.byeolnight.controller.user;

import com.byeolnight.dto.user.UserProfileDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/users")
@RequiredArgsConstructor
@Tag(name = "🌍 공개 API - 사용자", description = "비회원도 접근 가능한 사용자 정보 조회 API")
public class PublicUserController {

    private final UserService userService;

    @Operation(summary = "사용자 프로필 조회 (닉네임)", description = "닉네임으로 사용자 공개 프로필을 조회합니다.")
    @GetMapping("/profile/{nickname}")
    public ResponseEntity<CommonResponse<UserProfileDto>> getUserProfile(@PathVariable String nickname) {
        UserProfileDto profile = userService.getUserProfileByNickname(nickname);
        return ResponseEntity.ok(CommonResponse.success(profile));
    }

    @Operation(summary = "사용자 프로필 조회 (ID)", description = "사용자 ID로 공개 프로필을 조회합니다.")
    @GetMapping("/{userId}/profile")
    public ResponseEntity<CommonResponse<UserProfileDto>> getUserProfileById(@PathVariable Long userId) {
        UserProfileDto profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(CommonResponse.success(profile));
    }
}