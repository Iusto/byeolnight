
package com.byeolnight.application.user;

import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.entity.user.User.Role;
import com.byeolnight.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    public User register(String email, String password, String nickname, String phone) {
        if (!Boolean.TRUE.equals(redisTemplate.hasKey("email:verified:" + email))) {
            throw new IllegalStateException("이메일 인증이 필요합니다.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .nickname(nickname)
                .phone(phone)
                .role(Role.USER)
                .build();

        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
