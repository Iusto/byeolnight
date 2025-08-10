package com.byeolnight.repository.post;

import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.post.Post.Category;
import com.byeolnight.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;



@Repository
public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

    /**
     * [게시판 화면] 카테고리별 최신순 게시글 조회 (블라인드 포함)
     * - 조건: 삭제되지 않은 게시글
     * - 정렬: 작성일 내림차순
     */
    Page<Post> findByIsDeletedFalseAndCategoryOrderByCreatedAtDesc(Category category, Pageable pageable);

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
     * [게시판 - 추천순 정렬 시] 전체 게시글을 추천수 기준으로 정렬 (블라인드 포함)
     * - 조건: 삭제되지 않은 게시글
     * - 정렬: 추천수 내림차순
     */
    Page<Post> findByIsDeletedFalseAndCategoryOrderByLikeCountDesc(Category category, Pageable pageable);

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
    
    /**
     * [관리자 페이지용] 삭제된 게시글 목록
     */
    List<Post> findByIsDeletedTrueOrderByCreatedAtDesc();

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
     * 사용자별 작성 게시글 수 조회 (삭제되지 않은 것만)
     */
    long countByWriterAndIsDeletedFalse(User writer);

    /**
     * 사용자별 특정 카테고리 게시글 수 조회 (삭제되지 않은 것만)
     */
    long countByWriterAndCategoryAndIsDeletedFalse(User writer, Category category);
    
    /**
     * 사용자가 작성한 서로 다른 카테고리 수 조회
     */
    @Query("SELECT COUNT(DISTINCT p.category) FROM Post p WHERE p.writer = :writer AND p.isDeleted = false")
    long countDistinctCategoriesByWriter(@Param("writer") User writer);
    
    // 검색 기능 (블라인드 포함)
    Page<Post> findByTitleContainingAndCategoryAndIsDeletedFalse(String title, Post.Category category, Pageable pageable);
    Page<Post> findByContentContainingAndCategoryAndIsDeletedFalse(String content, Post.Category category, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) AND p.category = :category AND p.isDeleted = false")
    Page<Post> findByTitleOrContentContainingAndCategoryAndIsDeletedFalse(@Param("keyword") String keyword, @Param("category") Post.Category category, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.writer.nickname LIKE %:nickname% AND p.category = :category AND p.isDeleted = false")
    Page<Post> findByWriterNicknameContainingAndCategoryAndIsDeletedFalse(@Param("nickname") String nickname, @Param("category") Post.Category category, Pageable pageable);

    /**
     * 사용자별 작성 게시글 조회 (삭제되지 않은 것만)
     */
    Page<Post> findByWriterAndIsDeletedFalseOrderByCreatedAtDesc(User writer, Pageable pageable);

    
    /**
     * 토론 주제 관련 쿼리
     */
    List<Post> findByDiscussionTopicTrueAndPinnedTrue();
    
    List<Post> findByDiscussionTopicTrueAndCreatedAtAfter(java.time.LocalDateTime since);
    
    @Query("SELECT p FROM Post p WHERE p.discussionTopic = true AND p.pinned = true ORDER BY p.createdAt DESC")
    java.util.Optional<Post> findTodayDiscussionTopic();
    
    @Query("SELECT p FROM Post p WHERE p.originTopicId = :topicId AND p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<Post> findRelatedOpinionPosts(@Param("topicId") Long topicId, Pageable pageable);
    
    Page<Post> findByCategoryAndDiscussionTopicFalseAndIsDeletedFalse(Category category, Pageable pageable);
    
    /**
     * 30일 이상 지난 삭제된 게시글 조회
     */
    @Query("SELECT p FROM Post p WHERE p.isDeleted = true AND p.deletedAt < :threshold")
    List<Post> findExpiredDeletedPosts(@Param("threshold") LocalDateTime threshold);

    /**
     * 게시글 내용에 특정 문자열이 포함된 게시글 존재 여부 확인 (S3 파일 사용 여부 체크용)
     */
    boolean existsByContentContaining(String content);
    
    /**
     * 카테고리별 삭제되지 않은 게시글 개수 조회
     */
    long countByCategoryAndIsDeletedFalse(Category category);

    /**
     * 블라인드 처리되지 않고 삭제되지 않은 게시글만 조회 (사이트맵용)
     */
    Page<Post> findByBlindedFalseAndIsDeletedFalse(Pageable pageable);
}
