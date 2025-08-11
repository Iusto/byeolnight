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

    @Operation(summary = "사용자 프로필 조회 (닉네임)", description = """
    닉네임으로 사용자 공개 프로필을 조회합니다.
    
    📊 포함 정보:
    - 닉네임, 가입일
    - 작성한 게시글/댓글 수
    - 대표 인증서
    - 장착 아이콘
    
    👥 비회원도 접근 가능한 공개 API입니다.
    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/profile/{nickname}")
    public ResponseEntity<CommonResponse<UserProfileDto>> getUserProfile(
            @Parameter(description = "사용자 닉네임", example = "우주탐험가") @PathVariable String nickname) {
        UserProfileDto profile = userService.getUserProfileByNickname(nickname);
        return ResponseEntity.ok(CommonResponse.success(profile));
    }

    @Operation(summary = "사용자 프로필 조회 (ID)", description = """
    사용자 ID로 공개 프로필을 조회합니다.
    
    📊 닉네임 조회와 동일한 정보를 제공합니다.
    👥 비회원도 접근 가능한 공개 API입니다.
    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userId}/profile")
    public ResponseEntity<CommonResponse<UserProfileDto>> getUserProfileById(
            @Parameter(description = "사용자 ID", example = "123") @PathVariable Long userId) {
        UserProfileDto profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(CommonResponse.success(profile));
    }
}