package com.byeolnight.repository;

import com.byeolnight.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, Long> {
    
    boolean existsByVideoId(String videoId);
    
    boolean existsByTitle(String title);
    
    List<Cinema> findByCreatedAtAfter(LocalDateTime cutoffDate);
    
    List<Cinema> findTop10ByOrderByCreatedAtDesc();
    
    long countByCreatedAtAfter(LocalDateTime after);
}