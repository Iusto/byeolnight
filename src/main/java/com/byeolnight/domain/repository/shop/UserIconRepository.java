package com.byeolnight.domain.repository.shop;

import com.byeolnight.domain.entity.shop.StellaIcon;
import com.byeolnight.domain.entity.shop.StellaIconGrade;
import com.byeolnight.domain.entity.shop.UserIcon;
import com.byeolnight.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
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
    
    /**
     * 사용자 ID로 아이콘 수 조회
     */
    long countByUserId(Long userId);
    
    /**
     * 사용자 ID와 아이콘 등급으로 아이콘 수 조회
     */
    @Query("SELECT COUNT(ui) FROM UserIcon ui WHERE ui.user.id = :userId AND ui.stellaIcon.grade = :grade")
    long countByUserIdAndIconGrade(@Param("userId") Long userId, @Param("grade") StellaIconGrade grade);
    
    /**
     * 사용자 ID로 등급별 아이콘 수 조회
     */
    @Query("SELECT ui.stellaIcon.grade, COUNT(ui) FROM UserIcon ui WHERE ui.user.id = :userId GROUP BY ui.stellaIcon.grade")
    Map<StellaIconGrade, Long> countByUserIdGroupByGrade(@Param("userId") Long userId);
    
    /**
     * 전체 아이콘 수 조회
     */
    @Query("SELECT COUNT(si) FROM StellaIcon si")
    long countTotalIcons();
}