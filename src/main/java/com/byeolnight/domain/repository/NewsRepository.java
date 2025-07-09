package com.byeolnight.domain.repository;

import com.byeolnight.domain.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    
    boolean existsByTitle(String title);
    
    boolean existsByUrl(String url);
    
    List<News> findTop10ByCreatedAtAfterAndUsedForDiscussionFalseOrderByCreatedAtDesc(LocalDateTime after);
}