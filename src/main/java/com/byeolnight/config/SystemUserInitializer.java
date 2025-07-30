package com.byeolnight.config;

import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.shop.StellaIconRepository;
import com.byeolnight.domain.repository.user.UserRepository;
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

    @Value("${system.password.newsbot}")
    private String newsBotRawPassword;

    @Value("${system.password.system}")
    private String systemRawPassword;

    @Override
    public void run(ApplicationArguments args) {
        
        // 뉴스봇 사용자 생성
        if (!userRepository.existsByEmail("newsbot@byeolnight.com")) {
            String phone = "000-0000-0001";
            String encodedPassword = passwordEncoder.encode(newsBotRawPassword);
            
            User newsBot = User.builder()
                    .email("newsbot@byeolnight.com")
                    .nickname("뉴스봇")
                    .password(encodedPassword)
                    .phone(encryptionUtil.encrypt(phone))
                    .phoneHash(encryptionUtil.hashPhone(phone))
                    .role(User.Role.ADMIN)
                    .emailVerified(true)
                    .phoneVerified(true)
                    .build();

            userRepository.save(newsBot);
            log.info("뉴스봇 계정이 생성되었습니다: newsbot@byeolnight.com");
        } else {
            log.info("뉴스봇 계정이 이미 존재합니다.");
        }

        // 기존 시스템 계정 유지 (하위 호환성)
        if (!userRepository.existsByEmail("system@byeolnight.com")) {
            String phone = "000-0000-0000";
            String encodedPassword = passwordEncoder.encode(systemRawPassword);
            
            User systemUser = User.builder()
                    .email("system@byeolnight.com")
                    .nickname("별 헤는 밤")
                    .password(encodedPassword)
                    .phone(encryptionUtil.encrypt(phone))
                    .phoneHash(encryptionUtil.hashPhone(phone))
                    .role(User.Role.ADMIN)
                    .emailVerified(true)
                    .phoneVerified(true)
                    .build();

            userRepository.save(systemUser);
            log.info("시스템 사용자 계정이 생성되었습니다: system@byeolnight.com");
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