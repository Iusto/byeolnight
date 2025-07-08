package com.byeolnight.domain.repository.shop;

import com.byeolnight.domain.entity.shop.StellaIcon;
import com.byeolnight.domain.entity.shop.StellaIconGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StellaIconRepository extends JpaRepository<StellaIcon, Long> {
    
    /**
     * 구매 가능한 아이콘 목록 조회
     */
    List<StellaIcon> findByAvailableTrue();
    
    /**
     * 등급별 아이콘 조회
     */
    List<StellaIcon> findByGradeAndAvailableTrue(StellaIconGrade grade);
    
    /**
     * 이름으로 아이콘 조회
     */
    Optional<StellaIcon> findByName(String name);
}