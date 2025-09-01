package com.byeolnight.domain.weather.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "weather_observations")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherObservation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String location;
    
    @Column(nullable = false)
    private Double latitude;
    
    @Column(nullable = false)
    private Double longitude;
    
    @Column(nullable = false)
    private Double cloudCover; // 구름량 (0-100%)
    
    @Column(nullable = false)
    private Double visibility; // 시정 (km)
    
    @Column(nullable = false)
    private String moonPhase; // 달의 위상
    
    @Column(nullable = false)
    private String observationQuality; // EXCELLENT, GOOD, FAIR, POOR
    
    @Column(nullable = false)
    private LocalDateTime observationTime;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}