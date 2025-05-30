package com.byeolnight.controller;

import com.byeolnight.application.user.UserService;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.dto.user.UpdateProfileRequestDto;
import com.byeolnight.dto.user.UserResponseDto;
import com.byeolnight.dto.user.UserSignUpRequestDto;
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

    @PostMapping("/register")
    @Operation(summary = "회원가입", description = "회원가입을 수행합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = Long.class)))
    })
    public ResponseEntity<CommonResponse<Long>> register(@RequestBody @Valid UserSignUpRequestDto dto) {
        User user = userService.register(
                dto.getEmail(),
                dto.getPassword(),
                dto.getNickname(),
                dto.getPhone()
        );
        return ResponseEntity.ok(CommonResponse.success(user.getId()));
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
    public ResponseEntity<CommonResponse<UserResponseDto>> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(CommonResponse.success(UserResponseDto.from(user)));
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "회원 프로필 수정", description = "닉네임 및 전화번호를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공")
    })
    @PutMapping("/profile")
    public ResponseEntity<CommonResponse<Void>> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequestDto dto) {
        userService.updateProfile(user.getId(), dto);
        return ResponseEntity.ok(CommonResponse.success());
    }
}