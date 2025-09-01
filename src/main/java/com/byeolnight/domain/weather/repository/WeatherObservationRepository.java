package com.byeolnight.domain.weather.repository;

import com.byeolnight.domain.weather.entity.WeatherObservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface WeatherObservationRepository extends JpaRepository<WeatherObservation, Long> {
    
    @Query("SELECT w FROM WeatherObservation w WHERE w.latitude BETWEEN :lat - 0.1 AND :lat + 0.1 " +
           "AND w.longitude BETWEEN :lon - 0.1 AND :lon + 0.1 " +
           "AND w.observationTime > :since ORDER BY w.observationTime DESC")
    Optional<WeatherObservation> findRecentByLocation(@Param("lat") Double latitude, 
                                                     @Param("lon") Double longitude, 
                                                     @Param("since") LocalDateTime since);
}