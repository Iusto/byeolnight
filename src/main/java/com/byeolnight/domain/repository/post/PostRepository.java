package com.byeolnight.domain.repository.post;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.post.Post.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * [홈화면 / 전체 게시판] 최신순 전체 게시글 조회
     * - 조건: 삭제되지 않았고 블라인드되지 않은 게시글
     */
    Page<Post> findByIsDeletedFalseAndBlindedFalseOrderByCreatedAtDesc(Pageable pageable);

    /**
     * [게시판 화면] 카테고리별 최신순 게시글 조회
     * - 조건: 삭제되지 않았고 블라인드되지 않은 게시글
     * - 정렬: 작성일 내림차순
     */
    Page<Post> findByIsDeletedFalseAndBlindedFalseAndCategoryOrderByCreatedAtDesc(Category category, Pageable pageable);

    /**
     * [게시판 - 최신순 정렬 시] HOT 게시글 조회
     * - 조건:
     *    1. 삭제되지 않았고
     *    2. 블라인드되지 않았으며
     *    3. 특정 카테고리이고
     *    4. 한 달 이내 생성된 게시글 중
     *    5. 추천수 5 이상
     * - 정렬: 추천수 내림차순
     * - 제한: 상위 4개 (Pageable로 제한)
     */
    @Query("""
    SELECT p FROM Post p
    WHERE p.isDeleted = false
      AND p.blinded = false
      AND p.category = :category
      AND p.createdAt >= :threshold
      AND p.likeCount >= 5
    ORDER BY p.likeCount DESC
    """)
    List<Post> findTopHotPostsByCategory(@Param("category") Category category,
                                         @Param("threshold") LocalDateTime threshold,
                                         Pageable pageable);

    /**
     * [게시판 - 추천순 정렬 시] 전체 게시글을 추천수 기준으로 정렬
     * - 조건: 삭제되지 않고 블라인드되지 않은 게시글
     * - 정렬: 추천수 내림차순
     * - HOT 여부 무관, 날짜 제한 없음
     */
    Page<Post> findByIsDeletedFalseAndBlindedFalseAndCategoryOrderByLikeCountDesc(Category category, Pageable pageable);

    /**
     * [게시글 상세조회] 게시글 + 작성자 정보 즉시 로딩
     * - 조건: 삭제되지 않고 블라인드되지 않은 게시글
     * - 사용처: 게시글 상세 화면
     */
    @Query("""
    SELECT p FROM Post p
    JOIN FETCH p.writer
    WHERE p.id = :id AND p.isDeleted = false AND p.blinded = false
    """)
    Optional<Post> findWithWriterById(@Param("id") Long id);

    /**
     * [관리자 페이지용] 블라인드 처리된 게시글 목록
     * - 조건: 삭제되지 않고 블라인드 상태인 게시글
     */
    List<Post> findByIsDeletedFalseAndBlindedTrueOrderByCreatedAtDesc();

    @Query("""
    SELECT p FROM Post p
    WHERE p.isDeleted = false
      AND p.blinded = false
      AND p.createdAt >= :threshold
      AND p.likeCount >= 5
    ORDER BY p.likeCount DESC
    """)
    List<Post> findTopHotPostsAcrossAllCategories(@Param("threshold") LocalDateTime threshold, Pageable pageable);

    /**
     * 제목과 카테고리로 게시글 존재 여부 확인 (중복 방지용)
     */
    boolean existsByTitleAndCategory(String title, Category category);

    /**
     * 사용자별 작성 게시글 수 조회 (삭제되지 않은 것만)
     */
    long countByWriterAndIsDeletedFalse(com.byeolnight.domain.entity.user.User writer);

    /**
     * 사용자별 특정 카테고리 게시글 수 조회 (삭제되지 않은 것만)
     */
    long countByWriterAndCategoryAndIsDeletedFalse(com.byeolnight.domain.entity.user.User writer, Category category);
    
    /**
     * 중복 이벤트 검사: 동일 제목, 카테고리, 작성자, 특정 날짜 이후
     */
    boolean existsByTitleAndCategoryAndWriterAndCreatedAtAfter(
        String title, 
        Category category, 
        com.byeolnight.domain.entity.user.User writer, 
        LocalDateTime createdAt
    );

    /**
     * 신고된 게시글 조회 (신고 수 1개 이상)
     */
    @Query("""
    SELECT DISTINCT p FROM Post p
    JOIN PostReport pr ON pr.post = p
    WHERE p.isDeleted = false
    GROUP BY p
    HAVING COUNT(pr) > 0
    ORDER BY COUNT(pr) DESC, p.createdAt DESC
    """)
    List<Post> findReportedPosts();
    
    // 검색 기능
    Page<Post> findByTitleContainingAndCategoryAndIsDeletedFalseAndBlindedFalse(String title, Post.Category category, Pageable pageable);
    Page<Post> findByContentContainingAndCategoryAndIsDeletedFalseAndBlindedFalse(String content, Post.Category category, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) AND p.category = :category AND p.isDeleted = false AND p.blinded = false")
    Page<Post> findByTitleOrContentContainingAndCategory(@Param("keyword") String keyword, @Param("category") Post.Category category, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.writer.nickname LIKE %:nickname% AND p.category = :category AND p.isDeleted = false AND p.blinded = false")
    Page<Post> findByWriterNicknameContainingAndCategory(@Param("nickname") String nickname, @Param("category") Post.Category category, Pageable pageable);
}
