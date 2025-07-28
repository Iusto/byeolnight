package com.byeolnight.domain.repository;

import com.byeolnight.domain.entity.Suggestion;
import com.byeolnight.domain.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SuggestionRepository extends JpaRepository<Suggestion, Long> {

    // 카테고리별 조회
    Page<Suggestion> findByCategory(Suggestion.SuggestionCategory category, Pageable pageable);

    // 상태별 조회
    Page<Suggestion> findByStatus(Suggestion.SuggestionStatus status, Pageable pageable);

    // 카테고리와 상태별 조회
    Page<Suggestion> findByCategoryAndStatus(
            Suggestion.SuggestionCategory category, 
            Suggestion.SuggestionStatus status, 
            Pageable pageable
    );

    // 공개 건의사항만 조회
    Page<Suggestion> findByIsPublicTrue(Pageable pageable);
    
    // 공개 + 카테고리별 조회
    Page<Suggestion> findByCategoryAndIsPublicTrue(Suggestion.SuggestionCategory category, Pageable pageable);
    
    // 공개 + 상태별 조회
    Page<Suggestion> findByStatusAndIsPublicTrue(Suggestion.SuggestionStatus status, Pageable pageable);
    
    // 공개 + 카테고리 + 상태별 조회
    Page<Suggestion> findByCategoryAndStatusAndIsPublicTrue(
            Suggestion.SuggestionCategory category, 
            Suggestion.SuggestionStatus status, 
            Pageable pageable
    );

    // 작성자별 조회
    Page<Suggestion> findByAuthor(User author, Pageable pageable);
    
    // 사용자별 건의사항 작성 수 조회
    long countByAuthor(User author);
}