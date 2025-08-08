package com.byeolnight.repository.shop;

import com.byeolnight.entity.shop.StellaIcon;
import com.byeolnight.entity.shop.UserIcon;
import com.byeolnight.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserIconRepository extends JpaRepository<UserIcon, Long> {

    /**
     * 사용자의 모든 아이콘 조회 (StellaIcon fetch join)
     */
    @Query("SELECT ui FROM UserIcon ui JOIN FETCH ui.stellaIcon WHERE ui.user = :user ORDER BY ui.createdAt DESC")
    List<UserIcon> findByUserWithStellaIconOrderByCreatedAtDesc(@Param("user") User user);
    
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
    
    /**
     * 사용자 ID로 아이콘 수 조회
     */
    long countByUserId(Long userId);
}