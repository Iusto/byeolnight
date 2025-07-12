package com.byeolnight.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "news")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class News {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 500)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "image_url", length = 1000)
    private String imageUrl;
    
    @Column(nullable = false, length = 1000)
    private String url;
    
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    
    @Column(length = 200)
    private String hashtags;
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @Column(length = 100)
    private String source;
    
    @Column(name = "used_for_discussion")
    @Builder.Default
    private boolean usedForDiscussion = false;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public void setUsedForDiscussion(boolean usedForDiscussion) {
        this.usedForDiscussion = usedForDiscussion;
    }
}