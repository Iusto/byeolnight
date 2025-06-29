package com.byeolnight.controller.user;

import com.byeolnight.dto.user.UserProfileDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/users")
@RequiredArgsConstructor
public class PublicUserController {

    private final UserService userService;

    @Operation(summary = "사용자 프로필 조회", description = "닉네임으로 사용자 공개 프로필을 조회합니다.")
    @GetMapping("/profile/{nickname}")
    public ResponseEntity<CommonResponse<UserProfileDto>> getUserProfile(@PathVariable String nickname) {
        UserProfileDto profile = userService.getUserProfileByNickname(nickname);
        return ResponseEntity.ok(CommonResponse.success(profile));
    }
}