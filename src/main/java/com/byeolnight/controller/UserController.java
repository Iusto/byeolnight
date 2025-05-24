
package com.byeolnight.controller;

import com.byeolnight.application.user.UserService;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.dto.user.UserResponseDto;
import com.byeolnight.dto.user.UserSignUpRequestDto;
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
    public ResponseEntity<?> register(@RequestBody @Valid UserSignUpRequestDto dto) {
        User user = userService.register(
                dto.getEmail(),
                dto.getPassword(),
                dto.getNickname(),
                dto.getPhone()
        );
        return ResponseEntity.ok(user.getId());
    }


    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(UserResponseDto.from(user));
    }
}
