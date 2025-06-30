package com.byeolnight.config;

import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.infrastructure.security.EncryptionUtil;
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
    private final EncryptionUtil encryptionUtil;
    
    @PostConstruct
    public void createSystemUsers() {
        // 뉴스봇 사용자 생성
        if (!userRepository.existsByEmail("newsbot@byeolnight.com")) {
            String phone = "000-0000-0001";
            User newsBot = User.builder()
                    .email("newsbot@byeolnight.com")
                    .nickname("뉴스봇")
                    .password(passwordEncoder.encode("NEWSBOT_PASSWORD"))
                    .phone(encryptionUtil.encrypt(phone))
                    .phoneHash(encryptionUtil.hashPhone(phone))
                    .role(User.Role.ADMIN)
                    .emailVerified(true)
                    .phoneVerified(true)
                    .build();
            
            userRepository.save(newsBot);
            log.info("뉴스봇 계정이 생성되었습니다: newsbot@byeolnight.com");
        }
        
        // 우주전시회봇 사용자 생성
        if (!userRepository.existsByEmail("exhibitionbot@byeolnight.com")) {
            String phone = "000-0000-0002";
            User exhibitionBot = User.builder()
                    .email("exhibitionbot@byeolnight.com")
                    .nickname("우주전시회봇")
                    .password(passwordEncoder.encode("EXHIBITIONBOT_PASSWORD"))
                    .phone(encryptionUtil.encrypt(phone))
                    .phoneHash(encryptionUtil.hashPhone(phone))
                    .role(User.Role.ADMIN)
                    .emailVerified(true)
                    .phoneVerified(true)
                    .build();
            
            userRepository.save(exhibitionBot);
            log.info("우주전시회봇 계정이 생성되었습니다: exhibitionbot@byeolnight.com");
        }
        
        // 기존 시스템 계정 유지 (하위 호환성)
        if (!userRepository.existsByEmail("system@byeolnight.com")) {
            String phone = "000-0000-0000";
            User systemUser = User.builder()
                    .email("system@byeolnight.com")
                    .nickname("별밤시스템")
                    .password(passwordEncoder.encode("SYSTEM_ACCOUNT_PASSWORD"))
                    .phone(encryptionUtil.encrypt(phone))
                    .phoneHash(encryptionUtil.hashPhone(phone))
                    .role(User.Role.ADMIN)
                    .emailVerified(true)
                    .phoneVerified(true)
                    .build();
            
            userRepository.save(systemUser);
            log.info("시스템 사용자 계정이 생성되었습니다: system@byeolnight.com");
        }
    }
}