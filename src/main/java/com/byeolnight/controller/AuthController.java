
package com.byeolnight.controller;

import com.byeolnight.application.auth.EmailAuthService;
import com.byeolnight.application.auth.PhoneAuthService;
import com.byeolnight.application.auth.TokenService;
import com.byeolnight.application.user.UserService;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.dto.auth.*;
import com.byeolnight.infrastructure.security.TokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final TokenProvider tokenProvider;
    private final EmailAuthService emailAuthService;
    private final PhoneAuthService phoneAuthService;
    private final TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email,
                                   @RequestParam String password) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String accessToken = tokenProvider.createAccessToken(user);
        String refreshToken = tokenProvider.createRefreshToken(user);

        return ResponseEntity.ok().body("Access: " + accessToken + "\nRefresh: " + refreshToken);
    }

    @PostMapping("/email/send")
    public ResponseEntity<Void> sendEmailCode(@RequestBody @Valid EmailRequestDto dto) {
        emailAuthService.sendCode(dto.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/verify")
    public ResponseEntity<Boolean> verifyEmailCode(@RequestBody @Valid EmailVerifyRequestDto dto) {
        boolean isValid = emailAuthService.verifyCode(dto.getEmail(), dto.getCode());
        return ResponseEntity.ok(isValid);
    }

    @PostMapping("/phone/send")
    public ResponseEntity<Void> sendPhoneCode(@RequestBody @Valid PhoneRequestDto dto) {
        phoneAuthService.sendCode(dto.getPhone());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/phone/verify")
    public ResponseEntity<Boolean> verifyPhoneCode(@RequestBody @Valid PhoneVerifyRequestDto dto) {
        boolean isValid = phoneAuthService.verifyCode(dto.getPhone(), dto.getCode());
        return ResponseEntity.ok(isValid);
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<String> refreshAccessToken(@RequestBody @Valid TokenRefreshRequestDto dto) {
        String refreshToken = dto.getRefreshToken();

        if (!tokenProvider.validateRefreshToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }

        String email = tokenProvider.getEmail(refreshToken);
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String newAccessToken = tokenProvider.createAccessToken(user);
        return ResponseEntity.ok(newAccessToken);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid LogoutRequestDto dto) {
        String refreshToken = dto.getRefreshToken();
        String email = tokenProvider.getEmail(refreshToken);
        tokenService.delete(refreshToken, email);
        return ResponseEntity.ok().build();
    }
}

