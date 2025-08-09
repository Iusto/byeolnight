package com.byeolnight.service.user;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public User loadUserByUsername(String userId) throws UsernameNotFoundException {
        // JWT의 sub 필드에서 오는 사용자 ID로 조회
        try {
            Long id = Long.parseLong(userId);
            return userRepository.findById(id)
                    .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: ID=" + userId));
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("잘못된 사용자 ID 형식: " + userId);
        }
    }
}
