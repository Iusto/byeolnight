package com.byeolnight.service.user;

import com.byeolnight.entity.shop.StellaIcon;
import com.byeolnight.entity.shop.UserIcon;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.shop.StellaIconRepository;
import com.byeolnight.repository.shop.UserIconRepository;
import com.byeolnight.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 신규·기존 회원의 기본 소행성 아이콘 지급 정책만 담당한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultIconService {

    private final UserRepository userRepository;
    private final StellaIconRepository stellaIconRepository;
    private final UserIconRepository userIconRepository;

    @Transactional
    public void grant(User user) {
        try {
            StellaIcon asteroidIcon = stellaIconRepository.findByName("소행성")
                    .or(() -> stellaIconRepository.findByName("Asteroid"))
                    .orElse(null);
            if (asteroidIcon == null) {
                log.warn("기본 소행성 아이콘이 없어 지급을 건너뜁니다.");
                return;
            }

            if (!userIconRepository.existsByUserAndStellaIcon(user, asteroidIcon)) {
                userIconRepository.save(UserIcon.builder()
                        .user(user)
                        .stellaIcon(asteroidIcon)
                        .purchasePrice(0)
                        .build());
            }
            if (user.getEquippedIconId() == null) {
                user.equipIcon(asteroidIcon.getId(), asteroidIcon.getIconUrl());
                userRepository.save(user);
            }
        } catch (Exception exception) {
            // 부가 기능 실패가 회원가입을 막지는 않되 운영자가 원인을 추적할 수 있게 기록한다.
            log.error("기본 소행성 아이콘 지급 실패: {}", exception.getMessage(), exception);
        }
    }

    @Transactional
    public int migrateActiveUsers() {
        int processedCount = 0;
        for (User user : userRepository.findAll()) {
            if (user.getStatus() == User.UserStatus.ACTIVE) {
                grant(user);
                processedCount++;
            }
        }
        log.info("기본 소행성 아이콘 마이그레이션 완료: {}명", processedCount);
        return processedCount;
    }
}
