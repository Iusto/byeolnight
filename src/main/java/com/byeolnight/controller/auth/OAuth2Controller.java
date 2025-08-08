package com.byeolnight.controller.auth;

import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.entity.user.User;
import com.byeolnight.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/oauth")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final UserService userService;

    @PostMapping("/setup-nickname")
    public ResponseEntity<CommonResponse<User>> setupNickname(
            @AuthenticationPrincipal User user,
            @RequestBody SetupNicknameRequest request) {
        
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.fail("인증이 필요합니다"));
        }

        if (request.nickname() == null || request.nickname().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.fail("닉네임을 입력해주세요"));
        }

        if (userService.isNicknameDuplicated(request.nickname().trim())) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.fail("이미 사용 중인 닉네임입니다"));
        }

        // 닉네임 업데이트
        user.updateNickname(request.nickname().trim(), java.time.LocalDateTime.now());
        User updatedUser = userService.save(user);

        return ResponseEntity.ok(CommonResponse.success(updatedUser));
    }

    public record SetupNicknameRequest(String nickname) {}
}