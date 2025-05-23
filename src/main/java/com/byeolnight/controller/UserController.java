
package com.byeolnight.controller;

import com.byeolnight.application.user.UserService;
import com.byeolnight.domain.entity.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String email,
                                      @RequestParam String password,
                                      @RequestParam String nickname) {
        User user = userService.register(email, password, nickname);
        return ResponseEntity.ok(user.getId());
    }
}
