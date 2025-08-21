package com.byeolnight.service.user;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public User loadUserByUsername(String userId) throws UsernameNotFoundException {
        // JWT의 sub 필드에서 오는 사용자 ID로 조회
        try {
            Long id = Long.parseLong(userId);
            log.debug("🔍 사용자 조회 시도: ID={}", id);
            
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                log.error("❌ 사용자 조회 실패: ID={} (DB에 존재하지 않음)", id);
                throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: ID=" + userId);
            }
            
            // 탈퇴한 사용자는 인증 불가
            if (user.getStatus() == User.UserStatus.WITHDRAWN) {
                log.warn("⚠️ 탈퇴한 사용자 접근 시도: ID={}", id);
                throw new UsernameNotFoundException("탈퇴한 사용자입니다: ID=" + userId);
            }
            
            log.debug("✅ 사용자 조회 성공: ID={}, 이메일={}, 상태={}", id, user.getEmail(), user.getStatus());
            return user;
            
        } catch (NumberFormatException e) {
            log.error("❌ 잘못된 사용자 ID 형식: {}", userId);
            throw new UsernameNotFoundException("잘못된 사용자 ID 형식: " + userId);
        }
    }
}
