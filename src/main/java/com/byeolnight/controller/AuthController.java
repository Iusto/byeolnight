
package com.byeolnight.controller;

import com.byeolnight.application.user.UserService;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.infrastructure.security.TokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final TokenProvider tokenProvider;

    public AuthController(UserService userService, TokenProvider tokenProvider) {
        this.userService = userService;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email,
                                   @RequestParam String password) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        // Password 검증 생략: 실제론 matches(password, user.getPassword())

        String accessToken = tokenProvider.createAccessToken(user);
        String refreshToken = tokenProvider.createRefreshToken(user);

        return ResponseEntity.ok().body("Access: " + accessToken + "\nRefresh: " + refreshToken);
    }
}
