
package com.byeolnight.service.user;

import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.entity.user.User.Role;
import com.byeolnight.domain.repository.UserRepository;
import com.byeolnight.dto.user.UpdateProfileRequestDto;
import com.byeolnight.infrastructure.exception.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    public User register(String email, String password, String nickname, String phone) {
//        if (!Boolean.TRUE.equals(redisTemplate.hasKey("email:verified:" + email))) {
//            throw new IllegalStateException("이메일 인증이 필요합니다.");
//        }

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

    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        if (userRepository.existsByNickname(dto.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        if (!user.getNickname().equals(dto.getNickname())) {
            if (user.getNicknameUpdatedAt() != null &&
                    user.getNicknameUpdatedAt().isAfter(LocalDateTime.now().minusMonths(6))) {
                throw new IllegalStateException("닉네임은 6개월마다 변경할 수 있습니다.");
            }
        }

        user.updateProfile(dto.getNickname(), dto.getPhone(), LocalDateTime.now());
    }

}
