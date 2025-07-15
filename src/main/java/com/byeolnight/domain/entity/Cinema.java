package com.byeolnight.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "cinema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Cinema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, unique = true)
    private String videoId; // YouTube 비디오 ID

    @Column(nullable = false)
    private String videoUrl; // 전체 YouTube URL

    @Column(length = 100)
    private String channelTitle;

    @Column
    private LocalDateTime publishedAt; // YouTube 발행일

    @Column(columnDefinition = "TEXT")
    private String summary; // AI 생성 요약

    @Column(length = 500)
    private String hashtags;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Cinema(String title, String description, String videoId, String videoUrl,
                  String channelTitle, LocalDateTime publishedAt, String summary, String hashtags) {
        this.title = title;
        this.description = description;
        this.videoId = videoId;
        this.videoUrl = videoUrl;
        this.channelTitle = channelTitle;
        this.publishedAt = publishedAt;
        this.summary = summary;
        this.hashtags = hashtags;
    }
}