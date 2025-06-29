package com.byeolnight.domain.repository.shop;

import com.byeolnight.domain.entity.shop.StellaIcon;
import com.byeolnight.domain.entity.shop.UserIcon;
import com.byeolnight.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserIconRepository extends JpaRepository<UserIcon, Long> {
    
    /**
     * 사용자의 모든 아이콘 조회 (최신순)
     */
    List<UserIcon> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * 사용자의 모든 아이콘 조회
     */
    List<UserIcon> findByUser(User user);
    
    /**
     * 사용자가 특정 아이콘을 보유하고 있는지 확인
     */
    boolean existsByUserAndStellaIcon(User user, StellaIcon stellaIcon);
    
    /**
     * 사용자의 특정 아이콘 조회
     */
    Optional<UserIcon> findByUserAndStellaIcon(User user, StellaIcon stellaIcon);
    
    /**
     * 사용자의 현재 장착 중인 아이콘 조회
     */
    Optional<UserIcon> findByUserAndEquippedTrue(User user);
}