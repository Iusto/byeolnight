package com.byeolnight.config;

import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
@Slf4j
public class SystemUserInitializer {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @PostConstruct
    public void createSystemUsers() {
        // 뉴스봇 사용자 생성
        if (!userRepository.existsByEmail("newsbot@byeolnight.com")) {
            User newsBot = User.builder()
                    .email("newsbot@byeolnight.com")
                    .nickname("뉴스봇")
                    .password(passwordEncoder.encode("NEWSBOT_PASSWORD"))
                    .phone("000-0000-0001")
                    .role(User.Role.ADMIN)
                    .emailVerified(true)
                    .phoneVerified(true)
                    .build();
            
            userRepository.save(newsBot);
            log.info("뉴스봇 계정이 생성되었습니다: newsbot@byeolnight.com");
        }
        
        // 천문대봇 사용자 생성
        if (!userRepository.existsByEmail("observatorybot@byeolnight.com")) {
            User observatoryBot = User.builder()
                    .email("observatorybot@byeolnight.com")
                    .nickname("천문대봇")
                    .password(passwordEncoder.encode("OBSERVATORYBOT_PASSWORD"))
                    .phone("000-0000-0002")
                    .role(User.Role.ADMIN)
                    .emailVerified(true)
                    .phoneVerified(true)
                    .build();
            
            userRepository.save(observatoryBot);
            log.info("천문대봇 계정이 생성되었습니다: observatorybot@byeolnight.com");
        }
        
        // 기존 시스템 계정 유지 (하위 호환성)
        if (!userRepository.existsByEmail("system@byeolnight.com")) {
            User systemUser = User.builder()
                    .email("system@byeolnight.com")
                    .nickname("별밤시스템")
                    .password(passwordEncoder.encode("SYSTEM_ACCOUNT_PASSWORD"))
                    .phone("000-0000-0000")
                    .role(User.Role.ADMIN)
                    .emailVerified(true)
                    .phoneVerified(true)
                    .build();
            
            userRepository.save(systemUser);
            log.info("시스템 사용자 계정이 생성되었습니다: system@byeolnight.com");
        }
    }
}