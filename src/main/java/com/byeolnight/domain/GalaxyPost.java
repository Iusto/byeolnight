package com.byeolnight.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class GalaxyPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String content;
    private String imageUrl;

    private String galaxyType;
    private double confidence;

    private boolean userCorrected;
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters omitted for brevity
}
