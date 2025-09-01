package com.byeolnight.domain.weather.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "astronomy_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AstronomyEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String eventType; // METEOR_SHOWER, ECLIPSE, PLANET_CONJUNCTION
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false, length = 1000)
    private String description;
    
    @Column(nullable = false)
    private LocalDateTime eventDate;
    
    @Column(nullable = false)
    private LocalDateTime peakTime;
    
    @Column(nullable = false)
    private String visibility; // WORLDWIDE, NORTHERN_HEMISPHERE, SOUTHERN_HEMISPHERE, SPECIFIC_REGIONS
    
    @Column(nullable = false)
    private String magnitude; // HIGH, MEDIUM, LOW
    
    @Column(nullable = false)
    private Boolean isActive;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }
}