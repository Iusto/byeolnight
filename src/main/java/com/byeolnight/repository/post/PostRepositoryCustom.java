package com.byeolnight.repository.post;

import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.post.Post.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * QueryDSL을 사용한 Post 복잡 쿼리 인터페이스
 * Spring Data JPA 규칙: Repository명 + Custom
 */
public interface PostRepositoryCustom {
    
    /**
     * 동적 검색 쿼리 (제목, 내용, 작성자 복합 검색)
     */
    Page<Post> searchPosts(String keyword, Category category, String searchType, Pageable pageable);
    
    /**
     * HOT 게시글 조회 (복합 조건)
     */
    List<Post> findHotPosts(Category category, LocalDateTime threshold, int likeThreshold, int limit);
    
    /**
     * 사용자 활동 통계 (카테고리별 게시글 수)
     */
    List<PostCategoryStats> getUserPostStatsByCategory(Long userId);
    
    /**
     * 관리자 대시보드용 복합 조건 쿼리
     */
    Page<Post> findPostsForAdmin(PostAdminSearchCondition condition, Pageable pageable);
}

/**
 * 게시글 카테고리별 통계 DTO
 */
record PostCategoryStats(Category category, Long count) {}

/**
 * 관리자 검색 조건 DTO
 */
record PostAdminSearchCondition(
    String keyword,
    Category category,
    Boolean isDeleted,
    Boolean isBlinded,
    LocalDateTime startDate,
    LocalDateTime endDate
) {}