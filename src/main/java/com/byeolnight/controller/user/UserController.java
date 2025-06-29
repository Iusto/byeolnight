package com.byeolnight.controller.user;

import com.byeolnight.service.user.UserService;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.dto.user.UpdateProfileRequestDto;
import com.byeolnight.dto.user.UserResponseDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "내 정보 조회", description = "AccessToken을 통해 내 프로필 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "토큰 없음 또는 만료됨"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/me")
    public ResponseEntity<CommonResponse<UserResponseDto>> getCurrentUser(
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(CommonResponse.success(UserResponseDto.from(user)));
    }

    /**
     * 특정 사용자의 장착 중인 아이콘 조회
     */
    @GetMapping("/{userId}/equipped-icon")
    public ResponseEntity<CommonResponse<com.byeolnight.dto.shop.EquippedIconDto>> getUserEquippedIcon(@PathVariable Long userId) {
        com.byeolnight.dto.shop.EquippedIconDto equippedIcon = userService.getUserEquippedIcon(userId);
        return ResponseEntity.ok(CommonResponse.success(equippedIcon));
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "회원 프로필 수정", description = "닉네임 및 전화번호를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "유효성 검사 실패", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패 또는 토큰 만료", content = @Content),
            @ApiResponse(responseCode = "409", description = "중복 닉네임 또는 비밀번호 불일치", content = @Content)
    })
    @PutMapping("/profile")
    public ResponseEntity<CommonResponse<Void>> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequestDto dto) {
        userService.updateProfile(user.getId(), dto);
        return ResponseEntity.ok(CommonResponse.success());
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호를 확인하고 새 비밀번호로 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "유효성 검사 실패"),
            @ApiResponse(responseCode = "401", description = "현재 비밀번호 불일치")
    })
    @PutMapping("/password")
    public ResponseEntity<CommonResponse<Void>> changePassword(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Valid @RequestBody com.byeolnight.dto.user.PasswordChangeRequestDto dto) {
        userService.changePassword(user.getId(), dto);
        return ResponseEntity.ok(CommonResponse.success());
    }
}
