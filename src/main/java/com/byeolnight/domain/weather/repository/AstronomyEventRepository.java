package com.byeolnight.domain.weather.repository;

import com.byeolnight.domain.weather.entity.AstronomyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AstronomyEventRepository extends JpaRepository<AstronomyEvent, Long> {
    

    
    @Query("SELECT a FROM AstronomyEvent a WHERE a.isActive = true " +
           "AND a.eventDate >= :thirtyDaysAgo ORDER BY a.eventDate DESC")
    List<AstronomyEvent> findUpcomingEvents(@Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo);
    
    void deleteByEventType(String eventType);
    
    @Query("SELECT a FROM AstronomyEvent a WHERE a.isActive = true " +
           "AND a.eventDate >= :thirtyDaysAgo ORDER BY a.eventDate DESC")
    List<AstronomyEvent> findRecentEvents(@Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo);
}