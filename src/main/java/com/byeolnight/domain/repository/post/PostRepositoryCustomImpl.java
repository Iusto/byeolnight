package com.byeolnight.domain.repository.post;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.post.Post.Category;
import com.byeolnight.domain.entity.post.QPost;
import com.byeolnight.domain.entity.user.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * QueryDSL을 사용한 Post 복잡 쿼리 구현체
 * Spring Data JPA 규칙: Repository명 + CustomImpl
 */
@Repository
@RequiredArgsConstructor
public class PostRepositoryCustomImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QPost post = QPost.post;
    private final QUser user = QUser.user;

    @Override
    public Page<Post> searchPosts(String keyword, Category category, String searchType, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        
        // 기본 조건
        builder.and(post.isDeleted.eq(false));
        
        // 카테고리 조건
        if (category != null) {
            builder.and(post.category.eq(category));
        }
        
        // 검색 조건
        if (keyword != null && !keyword.trim().isEmpty()) {
            switch (searchType) {
                case "title" -> builder.and(post.title.containsIgnoreCase(keyword));
                case "content" -> builder.and(post.content.containsIgnoreCase(keyword));
                case "writer" -> builder.and(post.writer.nickname.containsIgnoreCase(keyword));
                default -> builder.and(post.title.containsIgnoreCase(keyword)
                        .or(post.content.containsIgnoreCase(keyword)));
            }
        }
        
        // 쿼리 실행
        List<Post> content = queryFactory
                .selectFrom(post)
                .leftJoin(post.writer, user).fetchJoin()
                .where(builder)
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        
        long total = queryFactory
                .selectFrom(post)
                .where(builder)
                .fetchCount();
        
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public List<Post> findHotPosts(Category category, LocalDateTime threshold, int likeThreshold, int limit) {
        BooleanBuilder builder = new BooleanBuilder();
        
        // 기본 조건
        builder.and(post.isDeleted.eq(false))
               .and(post.blinded.eq(false))
               .and(post.likeCount.goe(likeThreshold));
        
        // 선택적 조건
        if (category != null) {
            builder.and(post.category.eq(category));
        }
        if (threshold != null) {
            builder.and(post.createdAt.goe(threshold));
        }
        
        return queryFactory
                .selectFrom(post)
                .leftJoin(post.writer, user).fetchJoin()
                .where(builder)
                .orderBy(post.likeCount.desc(), post.createdAt.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<PostCategoryStats> getUserPostStatsByCategory(Long userId) {
        return queryFactory
                .select(Projections.constructor(PostCategoryStats.class,
                        post.category,
                        post.count(),
                        post.likeCount.sum().coalesce(0),
                        post.viewCount.sum().coalesce(0)
                ))
                .from(post)
                .where(post.writer.id.eq(userId)
                        .and(post.isDeleted.eq(false)))
                .groupBy(post.category)
                .orderBy(post.count().desc())
                .fetch();
    }

    @Override
    public Page<Post> findPostsForAdmin(PostAdminSearchCondition condition, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        
        // 동적 조건 추가 (기본 구현)
        if (condition != null) {
            // 삭제 상태
            builder.and(post.isDeleted.eq(true));
            
            // 기간 조건 (최근 30일)
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            builder.and(post.deletedAt.goe(thirtyDaysAgo));
        }
        
        List<Post> content = queryFactory
                .selectFrom(post)
                .leftJoin(post.writer, user).fetchJoin()
                .where(builder)
                .orderBy(post.deletedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        
        long total = queryFactory
                .selectFrom(post)
                .where(builder)
                .fetchCount();
        
        return new PageImpl<>(content, pageable, total);
    }
}