package com.byeolnight.config;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.shop.StellaIconRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.infrastructure.security.EncryptionUtil;
import com.byeolnight.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;

@Component
@RequiredArgsConstructor
@Slf4j
public class SystemUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionUtil encryptionUtil;
    private final UserService userService;
    private final StellaIconRepository stellaIconRepository;

    @Value("${app.system.users.newsbot.email:newsbot@byeolnight.com}")
    private String newsbotEmail;
    
    @Value("${app.system.users.system.email:system@byeolnight.com}")
    private String systemEmail;

    @Value("${app.system.passwords.newsbot}")
    private String newsBotRawPassword;

    @Value("${app.system.passwords.system}")
    private String systemRawPassword;

    @Override
    public void run(ApplicationArguments args) {
        
        // 뉴스봇 사용자 생성
        if (!userRepository.existsByEmail(newsbotEmail)) {
            String encodedPassword = passwordEncoder.encode(newsBotRawPassword);
            
            User newsBot = User.builder()
                    .email(newsbotEmail)
                    .nickname("뉴스봇")
                    .password(encodedPassword)
                    .role(User.Role.ADMIN)
                    .build();

            userRepository.save(newsBot);
            log.info("뉴스봇 계정이 생성되었습니다: {}", newsbotEmail);
        } else {
            log.info("뉴스봇 계정이 이미 존재합니다.");
        }

        // 기존 시스템 계정 유지 (하위 호환성)
        if (!userRepository.existsByEmail(systemEmail)) {
            String encodedPassword = passwordEncoder.encode(systemRawPassword);
            
            User systemUser = User.builder()
                    .email(systemEmail)
                    .nickname("별 헤는 밤")
                    .password(encodedPassword)
                    .role(User.Role.ADMIN)
                    .build();

            userRepository.save(systemUser);
            log.info("시스템 사용자 계정이 생성되었습니다: {}", systemEmail);
        } else {
            log.info("시스템 사용자 계정이 이미 존재합니다.");
        }
        
        // 기본 소행성 아이콘 마이그레이션 (시스템 시작 시 한 번만 실행)
        try {
            int maxRetries = 10;
            int retryCount = 0;
            
            while (retryCount < maxRetries) {
                if (stellaIconRepository.count() > 0) {
                    log.info("스텔라 아이콘 데이터 로드 완료 확인. 마이그레이션 시작.");
                    break;
                }
                Thread.sleep(500);
                retryCount++;
            }
            
            userService.migrateDefaultAsteroidIcon();
        } catch (Exception e) {
            log.warn("기본 소행성 아이콘 마이그레이션 실패: {}", e.getMessage());
        }
    }
}