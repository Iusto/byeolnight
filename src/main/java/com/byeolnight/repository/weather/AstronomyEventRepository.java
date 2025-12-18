package com.byeolnight.repository.weather;

import com.byeolnight.entity.weather.AstronomyEvent;
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
    
    @Query("SELECT a FROM AstronomyEvent a WHERE a.eventDate >= :startDate AND a.eventDate <= :endDate AND a.isActive = true ORDER BY a.eventDate ASC")
    List<AstronomyEvent> findEventsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(a) FROM AstronomyEvent a WHERE a.createdAt >= :startDate")
    long countByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);
}