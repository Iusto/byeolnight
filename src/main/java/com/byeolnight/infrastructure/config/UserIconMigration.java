package com.byeolnight.infrastructure.config;

/**
 * 사용자 아이콘 데이터 마이그레이션
 * 
 * 역할:
 * - 기존 사용자의 장착 아이콘 데이터 업데이트
 * - equippedIconId만 있고 equippedIconName이 누락된 데이터 보정
 * - StellaIcon 엔티티에서 iconUrl 정보를 가져와서 업데이트
 * - Order(2)로 StellaIconDataLoader 이후에 실행
 */

import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.entity.shop.StellaIcon;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.domain.repository.shop.StellaIconRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2) // StellaIconDataLoader 이후에 실행
public class UserIconMigration implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StellaIconRepository stellaIconRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("사용자 아이콘 이름 마이그레이션 시작...");
        
        List<User> usersWithEquippedIcon = userRepository.findByEquippedIconIdIsNotNull();
        
        for (User user : usersWithEquippedIcon) {
            if (user.getEquippedIconName() == null && user.getEquippedIconId() != null) {
                stellaIconRepository.findById(user.getEquippedIconId())
                    .ifPresent(icon -> {
                        user.equipIcon(user.getEquippedIconId(), icon.getIconUrl());
                        userRepository.save(user);
                        log.info("사용자 {} 아이콘 이름 업데이트: {}", user.getNickname(), icon.getIconUrl());
                    });
            }
        }
        
        log.info("사용자 아이콘 이름 마이그레이션 완료");
    }
}