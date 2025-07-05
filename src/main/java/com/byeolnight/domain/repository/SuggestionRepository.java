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

    // 작성자별 조회
    Page<Suggestion> findByAuthor(User author, Pageable pageable);

    // 제목으로 검색
    @Query("SELECT s FROM Suggestion s WHERE s.title LIKE %:keyword% ORDER BY s.createdAt DESC")
    Page<Suggestion> findByTitleContaining(@Param("keyword") String keyword, Pageable pageable);

    // 최신 건의사항 조회 (관리자용)
    @Query("SELECT s FROM Suggestion s WHERE s.status = 'PENDING' ORDER BY s.createdAt DESC")
    Page<Suggestion> findPendingSuggestions(Pageable pageable);

    // 통계용 - 상태별 개수
    long countByStatus(Suggestion.SuggestionStatus status);

    // 통계용 - 카테고리별 개수
    long countByCategory(Suggestion.SuggestionCategory category);
}